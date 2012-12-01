/*
 * Copyright 2011-2012 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.github.rfqu.df4j.nio;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;

import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.core.DataflowVariable;
import com.github.rfqu.df4j.core.Link;
import com.github.rfqu.df4j.core.Port;
import com.github.rfqu.df4j.core.Promise;
import com.github.rfqu.df4j.core.StreamPort;

/**
 * Asynchronously executes I/O socket requests using {@link java.nio.channels.Selector}.
 * Simplifies input-output, handling queues of I/O requests.
 * 
 * Internally, manages 2 actors: one for reading requests and one for writing requests.
 * After request is served, it is sent to the port denoted by <code>replyTo</code> parameter in
 * the read/write methods.
 */
public class AsyncSocketChannel extends Link
    implements StreamPort<SocketIORequest<?>>, SelectorEventListener
{
	private SelectorThread selectorThread = SelectorThread
			.getCurrentSelectorThread();
	protected volatile SocketChannel socketChannel;
	/** for client-side socket: signals connection completion */
	private Promise<SocketChannel> connEvent = new Promise<SocketChannel>();
	/** read requests queue */
	protected final ReaderQueue reader = new ReaderQueue();
	/** write requests queue */
	protected final WriterQueue writer = new WriterQueue();
	/** closes channel */
	protected Completer completer = new Completer();
	protected volatile boolean closed = false;

	/**
	 * for server-side socket
	 * 
	 * @param assch
	 * @throws IOException
	 */
	public AsyncSocketChannel(SocketChannel channel) throws IOException {
		channel.configureBlocking(false);
		init(channel);
	}

	void init(SocketChannel channel) {
		socketChannel = channel;
		reader.startXChange();
		writer.startXChange();
		connEvent.send(channel);
	}

	/**
	 * for client-side socket Starts connection to a server. IO requests can be
	 * queued immediately, but will be executed only after connection completes.
	 * If interested in the moment when connection is established, add a
	 * listener.
	 * 
	 * @throws IOException
	 */
	public AsyncSocketChannel(SocketAddress addr) throws IOException {
		// Create a non-blocking socket channel
		SocketChannel channel = SocketChannel.open();
		channel.configureBlocking(false);

		boolean connected = channel.connect(addr);
		if (connected) {
			init(channel);
		} else {
			// Kick off connection establishment
			selectorThread.register(channel, SelectionKey.OP_CONNECT,
					this);
		}
	}

	public <R extends Callback<SocketChannel>> R addConnListener(R listener) {
		connEvent.addListener(listener);
		return listener;
	}

	public SocketChannel getChannel() {
		return socketChannel;
	}

	public boolean isConnected() {
		return socketChannel != null;
	}

	public boolean isClosed() {
		return closed;
	}

	// ================== StreamPort I/O interface

	@Override
	public void send(SocketIORequest<?> request) {
		(request.isReadOp() ? reader : writer).send(request);
	}

	/**
	 * disallows subsequent posts of requests; already posted requests would be
	 * processed.
	 */
	@Override
	public void close() {
	    if (closed) {
	        return;
	    }
		closed = true;
		reader.close();
		writer.close();
		if (socketChannel==null) { // this may happen if connection rejected
		    return;
		}
        try {
            socketChannel.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            socketChannel=null;
        }
	}

	// ================== conventional I/O interface

	public <R extends SocketIORequest<R>> void write(R request, Port<R> replyTo) {
		request.prepareWrite(replyTo);
		send(request);
	}

	public <R extends SocketIORequest<R>> void write(R request,
			Port<R> replyTo, long timeout) {
		request.prepareWrite(replyTo, timeout);
		send(request);
	}

	public <R extends SocketIORequest<R>> void read(R request, Port<R> replyTo) {
		request.prepareRead(replyTo);
		send(request);
	}

	public <R extends SocketIORequest<R>> void read(R request, Port<R> replyTo,
			long timeout) {
		request.prepareRead(replyTo, timeout);
		send(request);
	}

	// ===================== inner classes

	abstract class RequestQueue
	    implements Port<SocketIORequest<?>>, Runnable
	{
	    ArrayDeque<SocketIORequest<?>> queue=new ArrayDeque<SocketIORequest<?>>();
	    volatile boolean closed=false;
	    
        @Override
        public synchronized void send(SocketIORequest<?> request) {
            if (!request.getBuffer().hasRemaining()) {
                request.failed(new IllegalArgumentException());
                return;
            }
            if (closed) { // TODO separate closing
                request.failed(new AsynchronousCloseException());
                return;
            }
            if (socketChannel==null) {
                // not yet connected
                queue.add(request);
                return;
            }
            if (queue.isEmpty()) {
                int op=tryXChange(request);
                if (op!=0) {
                    selectorThread.execute(this);
                }
            } else {
                queue.add(request);
            }
        }

        /** called just after initialization,
         * in case some requests submitted before connection completion
         * @param op
         */
        void startXChange() {
            SocketIORequest<?> request = queue.poll();
            if (request==null) {
                // nothing to process
                return;
            }
            int op=tryXChange(request);
            if (op!=0) {
                selectorThread.execute(this);
            }
        }

        /**
         * Called on selector thread
         */
        @Override
        public void run() {
            SocketIORequest<?> request = queue.poll();
            if (request==null) {
                return;
            }
            int op=tryXChange(request);
            if (op!=0) {
                try {
                    selectorThread.registerNow(socketChannel, op, AsyncSocketChannel.this);
                } catch (ClosedChannelException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    close();
                }
            }
        }

        /**
         * Called on selector thread
         * @param key selector key from Selector
         * @param op interest operation to switch off, if needed
         */
        void resume(int op) {
            SocketIORequest<?> request = queue.poll();
            if (request==null) {
                // nothing to process
                selectorThread.interestOff(socketChannel, op);
                return;
            }
            int interest=tryXChange(request);
            if (interest==0 && socketChannel!=null) {
                selectorThread.interestOff(socketChannel, op);
            } // else required interest op is set already
        }

        void close() {
            closed=true;
        }
        
        /** make as many exchanges as possible
         * @param request
         * @return 0 if all requests processed
         *         else interest operation to register on selector
         */
        abstract int tryXChange(SocketIORequest<?> request);
 	}

	class ReaderQueue extends RequestQueue {
	    /**
	     * @param request
	     * @return interest op to register 
	     */
		@Override
        public int tryXChange(SocketIORequest<?> request) {
            for (;;) {
                ByteBuffer readBuffer = request.getBuffer();
                // Attempt to read off the channel
                int numRead;
                try {
                    numRead = socketChannel.read(readBuffer);
                } catch (IOException exc) {
                    request.replyFailure(exc);
                    AsyncSocketChannel.this.close();
                    return 0;
                }
                if (numRead == -1) {
                    // Remote entity shut the socket down cleanly. Do the
                    // same from our end and cancel the channel.
                    request.completed(-1); // TODO define how to signal end of stream
                    close();
                } else if (!request.getBuffer().hasRemaining()) {
                    // buffer is full;
                    request.completed(numRead);
                    // continue reading to the next buffer, if any
                } else if (numRead == 0) {
                    // no more data available, put request back into queue,
                    // and order to wait next signal from selector,
                    // then repeat attempt to read to the same buffer
                    queue.addFirst(request);
                    return SelectionKey.OP_READ;
                } else {
                    request.completed(numRead);
                    // continue reading to the next buffer, if any
                }
                request=queue.poll();
                if (request==null) {
                    return 0;
                }
            }

		}

        public void resume() {
            resume(SelectionKey.OP_READ);
        }

        @Override
		protected void close() {
            if (closed) {
                return;
            }
            super.close();
			completer.readerFinished.up();
		}

	}

	class WriterQueue extends RequestQueue {
		/**
         * @return interest op to register 
		 */
		@Override
		protected int tryXChange(SocketIORequest<?> request) {
			for (;;) {
                ByteBuffer writeBuffer = request.getBuffer();
                int numWrit;
                try {
                    numWrit = socketChannel.write(writeBuffer);
                } catch (IOException exc) {
                    request.replyFailure(exc);
                    close();
                    return 0;
                }
                if (numWrit == -1) {
                    // Remote entity shut the socket down cleanly. Do the
                    // same from our end and cancel the channel.
                    request.completed(-1); // TODO define how to signal end of stream
                    close();
                    return 0;
                } else if (!request.getBuffer().hasRemaining()) {
                    // buffer is full;
                    request.completed(numWrit);
                    // continue writing of the next buffer, if any
                    request=queue.poll();
                    if (request==null) {
                        return 0;
                    }
                } else {
                    // not all data from buffer written
                    // put request back into queue,
                    // and order to wait next signal from selector,
                    // then repeat attempt to read to the same buffer
                    queue.addFirst(request);
                    return SelectionKey.OP_WRITE;
                }
            }
		}

        @Override
        protected void close() {
            if (closed) {
                return;
            }
            super.close();
            completer.writerFinished.up();
        }

        public void resume() {
            resume(SelectionKey.OP_WRITE);
        }
	}

	/** called on selector thread
	 * 
	 */
    @Override
    public void onSelectorEvent(SelectionKey key) {
        if (key.isConnectable()) {
            try {
                SocketChannel channel = (SocketChannel) key.channel();
                channel.finishConnect();
                selectorThread.interestOff(channel, SelectionKey.OP_ACCEPT);
                init(channel);
            } catch (IOException e) {
                e.printStackTrace();
                AsyncSocketChannel.this.close(); // TODO send failure
            }
        }
        if (key.isValid()&& key.isReadable()) {
            reader.resume();
        }
        if (key.isValid()&& key.isWritable()) {
            writer.resume();
        }
     }

	/**
	 * closes underlying SocketChannel after all requests has been processed.
	 */
	class Completer extends DataflowVariable {
		final Semafor readerFinished = new Semafor();
		final Semafor writerFinished = new Semafor();

		@Override
		protected void act() {
		    AsyncSocketChannel.this.close();
		}

	}

}
