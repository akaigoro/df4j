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
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import com.github.rfqu.df4j.core.Actor;
import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.core.DataflowVariable;
import com.github.rfqu.df4j.core.Link;
import com.github.rfqu.df4j.core.Port;
import com.github.rfqu.df4j.core.Promise;
import com.github.rfqu.df4j.core.StreamPort;
import com.github.rfqu.df4j.core.Task;

/**
 * Asynchronously executes I/O socket requests using {@link java.nio.channels.Selector}.
 * Simplifies input-output, handling queues of I/O requests.
 * 
 * Internally, manages 2 actors: one for reading requests and one for writing requests.
 * After request is served, it is sent to the port denoted by <code>replyTo</code> parameter in
 * the read/write methods.
 */
public class AsyncSocketChannel extends Link
    implements StreamPort<SocketIORequest<?>>
{
	private SelectorThread selectorThread = SelectorThread
			.getCurrentSelectorThread();
	protected volatile SocketChannel socketChannel;
	/** for client-side socket: signals connection completion */
	private final Promise<SocketChannel> connEvent = new Promise<SocketChannel>();
	private final SelectorListener selectorListener=new SelectorListener();
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

	void init(SocketChannel channel) throws SocketException {
	    channel.socket().setTcpNoDelay(true);
	    socketChannel = channel;
		reader.resume();
		writer.resume();
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
	public AsyncSocketChannel(final SocketAddress addr) throws IOException {
		// Create a non-blocking socket channel
		final SocketChannel channel = SocketChannel.open();
		channel.configureBlocking(false);
		selectorThread.execute(new Task(){
            @Override
            public void run() {
                boolean connected;
                try {
                    connected = channel.connect(addr);
                    if (connected) {
                        init(channel);
                    } else {
                        // Kick off connection establishment
                        selectorThread.registerNow(channel, SelectionKey.OP_CONNECT,
                                selectorListener);
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
         });
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

    void interestOn(int op) throws ClosedChannelException {
        selectorThread.registerNow(socketChannel, op, selectorListener);
    }

    void interestOff(int op) throws ClosedChannelException {
        selectorThread.interestOff(socketChannel, op);
    }

    abstract class RequestQueue  extends Actor<SocketIORequest<?>> {
        
	    public RequestQueue() {
            super(selectorThread);
        }

        /** is on when resources from network are available */
	    Lockup net=new Lockup();

        public void resume() {
            net.on();
            try {
                interestOff(getSelectionKeyOp());
            } catch (ClosedChannelException e) {
            }
        }

        @Override
        public synchronized void send(SocketIORequest<?> request) {
            if (!request.getBuffer().hasRemaining()) {
                request.failed(new IllegalArgumentException());
                return;
            }
            super.send(request);
        }
        
        /** called when a request could not be executed immediately */
        void interestOn() throws ClosedChannelException {
            net.off(); // do not react on next request
            AsyncSocketChannel.this.interestOn(getSelectionKeyOp());  // until selector notifies
        }

        abstract int getSelectionKeyOp(); 
 	}

	class ReaderQueue extends RequestQueue {
        int getSelectionKeyOp() { 
            return SelectionKey.OP_READ;
        }
	    /**
	     * @param request
	     * @throws ClosedChannelException 
	     */
		@Override
        public void act(SocketIORequest<?> request) {
            ByteBuffer readBuffer = request.getBuffer();
            // Attempt to read off the channel
            int numRead;
            try {
                numRead = socketChannel.read(readBuffer);
            } catch (IOException exc) {
                request.replyFailure(exc);
                close(); // what TODO with the remaining requests?
                return;
            }
            if (numRead == -1) {
                // Remote entity shut the socket down cleanly. Do the
                // same from our end and cancel the channel.
                request.completed(-1); // TODO define how to signal end of stream
                close(); // what TODO with the remaining requests?
            } else if (!request.getBuffer().hasRemaining()) {
                // buffer is full;
                request.completed(numRead);
            } else if (numRead == 0) {
                // no data available, put request back into queue,
                // and order to wait next signal from selector,
                // then repeat attempt to read to the same buffer
                try {
                    interestOn();
                    pushback();
                } catch (ClosedChannelException e) {
                    request.failed(e);
                    close(); // what TODO with the remaining requests?
                }
            } else {
                request.completed(numRead);
            }
		}
		
        @Override
        protected void complete() throws Exception {
            completer.readerFinished.up();
        }
	}

	class WriterQueue extends RequestQueue {
	    int getSelectionKeyOp() { 
	        return SelectionKey.OP_WRITE;
	    }

		@Override
		protected void act(SocketIORequest<?> request) {
            ByteBuffer writeBuffer = request.getBuffer();
            int numWrit;
            try {
                numWrit = socketChannel.write(writeBuffer);
            } catch (IOException exc) {
                request.replyFailure(exc);
                close();
                return;
            }
            if (numWrit == -1) {
                // Remote entity shut the socket down cleanly. Do the
                // same from our end and cancel the channel.
                request.completed(-1); // TODO define how to signal end of stream
                close();
            } else if (!request.getBuffer().hasRemaining()) {
                // all data from buffer written to socket
                request.completed(numWrit);
            } else {
                // not all data from buffer written
                // put request back into queue,
                // and order to wait next signal from selector,
                // then repeat attempt to read to the same buffer
                try {
                    interestOn();
                    pushback();
                } catch (ClosedChannelException e) {
                    request.failed(e);
                    close(); // what TODO with the remaining requests?
                }
            }
		}

        @Override
        protected void complete() throws Exception {
            completer.writerFinished.up();
        }
	}
	
	class SelectorListener implements SelectorEventListener {
	    /** called on selector thread
	     * 
	     */
	    @Override
	    public void onSelectorEvent(SelectionKey key) {
	        if (key.isConnectable()) {
	            try {
	                SocketChannel channel = (SocketChannel) key.channel();
	                channel.finishConnect();
	                selectorThread.interestOff(channel, SelectionKey.OP_CONNECT);
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
