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

import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.core.Link;
import com.github.rfqu.df4j.core.Port;
import com.github.rfqu.df4j.core.Promise;
import com.github.rfqu.df4j.ext.ActorVariableLQ;

/**
 * Asynchronously executes I/O socket requests
 * using {@link java.nio.channels.Selector}.
 */
public class AsyncSocketChannel extends Link 
   implements Port<SocketIORequest<?>>
{
    private SocketChannel socketChannel;
    /** for client-side socket: signals connection completion */
    private Promise<SocketChannel> connEvent=new Promise<SocketChannel>();
    /** read requests queue */
    protected final ReadRequestQueue reader=new ReadRequestQueue();
    /** write requests queue */
    protected final WriteRequestQueue writer=new WriteRequestQueue();
    protected volatile boolean closed=false;
    private MyListener selectorListener=new MyListener();
    private SelectorThread currentSelectorThread=SelectorThread.getCurrentSelectorThread();

    /**
     * for server-side socket
     * @param sch
     * @throws IOException 
     */
    public AsyncSocketChannel(SocketChannel sch) throws IOException {
        sch.configureBlocking(false);
        currentSelectorThread
            .register(sch, SelectionKey.OP_READ|SelectionKey.OP_READ, selectorListener);
        
        synchronized(this) {
            socketChannel=sch;
        }
        connEvent.send(sch);
    }
    
    /**
     * for client-side socket
     * Starts connection to a server.
     * IO requests can be queued immediately,
     * but will be executed only after connection completes.
     * If interested in the moment when connection is established,
     * add a listener. 
     * @throws IOException
     */
    public AsyncSocketChannel(SocketAddress addr) throws IOException {
        // Create a non-blocking socket channel
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
    
        boolean connected = channel.connect(addr);    
        if (!connected){
        	// Kick off connection establishment
        	currentSelectorThread.register(channel, SelectionKey.OP_CONNECT, selectorListener);
        }
    }

    // ================== I/O
    
    @Override
	public void send(SocketIORequest<?> request) {
		if (closed) {
			request.failed(new ClosedChannelException());
			return;
		}
		(request.isReadOp()?reader:writer).send(request);
	}    

    public <R extends SocketIORequest<R>>void write(R request, Port<R> replyTo) {
        request.prepareWrite(replyTo);
        send(request);
    }

    public <R extends SocketIORequest<R>>void write(R request, Port<R> replyTo, long timeout) {
        request.prepareWrite(replyTo, timeout);
        send(request);
    }

    public <R extends SocketIORequest<R>>void read(R request, Port<R> replyTo) {
        request.prepareRead(replyTo);
        send(request);
    }

    public <R extends SocketIORequest<R>>void read(R request, Port<R> replyTo, long timeout) {
        request.prepareRead(replyTo, timeout);
        send(request);
    }

    public void close() {
        closed=true;
        if (socketChannel!=null) {
            currentSelectorThread.deregister(socketChannel);
            try {
                socketChannel.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                socketChannel=null;
            }
        }
        currentSelectorThread=null;
    }

    public synchronized void addListener(Callback<SocketChannel> listener) {
        connEvent.addListener(listener);
    }

    public SocketChannel getChannel() {
        return socketChannel;
    }

    public boolean isConnected() {
        return socketChannel!=null;
    }

    public boolean isClosed() {
        return closed;
    }

	//========================= backend inner classes
    
    /** wrapped in a separate class in order not to expose
     * callback methods
     */
    class MyListener implements SocketEventListener{
        /**
         * callback method for successful connection in client-side mode
         */
        @Override
        public void connect(SelectionKey key) {
            try {
                synchronized(this) {
                    socketChannel=(SocketChannel) key.channel();
                }
                socketChannel.finishConnect();
//                selectorKey.interestOps(SelectionKey.OP_READ|SelectionKey.OP_READ);
                currentSelectorThread.setInterest(socketChannel, SelectionKey.OP_CONNECT);
                connEvent.send(socketChannel);           
            } catch (IOException e) {
                e.printStackTrace();
                AsyncSocketChannel.this.close();
            }
        }

        @Override
        public void read(SelectionKey key) {
            reader.resume();
        }

        @Override
        public void write(SelectionKey key) {
            writer.resume();
        }

        @Override
        public void accept(SelectionKey key) {}

        @Override
        public void close() {
            AsyncSocketChannel.this.close();
        }
    }
    
    class ReadRequestQueue extends ActorVariableLQ<SocketIORequest<?>> {
        protected Semafor channelAcc=new Semafor(); // channel accessible for reading

        public void resume() {
            channelAcc.up();
        }

        /**
         * Both the request and channel are ready
         */
        @Override
        protected void act(SocketIORequest<?> request) throws Exception {
            if (AsyncSocketChannel.this.closed) {
                request.failed(new AsynchronousCloseException());
                return;
            }
            ByteBuffer readBuffer=request.getBuffer();

            // Attempt to read off the channel
            int numRead;
            try {
                numRead = socketChannel.read(readBuffer);
            } catch (IOException exc) {
                request.replyFailure(exc);
                AsyncSocketChannel.this.close();
                return;
            }

            if (numRead == -1) {
                // Remote entity shut the socket down cleanly. Do the
                // same from our end and cancel the channel.
                request.reply(-1); // TODO define how to signal end of stream
                AsyncSocketChannel.this.close();
            } else if (numRead == 0) {
                pushback(); // and wait next signal from selector
            } else {
//                if (readBuffer.hasRemaining())
                request.reply(numRead);
            }

        }
    }

    class WriteRequestQueue extends ActorVariableLQ<SocketIORequest<?>> {
        protected Semafor channelAcc=new Semafor(); // channel accessible for writing

        public void resume() {
            channelAcc.up();
        }

        /**
         * Both the request and channel are ready
         */
        @Override
        protected void act(SocketIORequest<?> request) throws Exception {
            if (AsyncSocketChannel.this.closed) {
                request.failed(new AsynchronousCloseException());
                return;
            }
            ByteBuffer writeBuffer=request.getBuffer();

            int totalWrit=0;
            while (writeBuffer.hasRemaining()) {
                int numWrit;
                try {
                    numWrit = socketChannel.write(writeBuffer);
                } catch (IOException exc) {
                    request.replyFailure(exc);
                    AsyncSocketChannel.this.close();
                    return;
                }
                if (numWrit == -1) {
                    // Remote entity shut the socket down cleanly. Do the
                    // same from our end and cancel the channel.
                    request.reply(-1); // TODO define how to signal end of stream
                    AsyncSocketChannel.this.close();
                    return;
                }
                if (numWrit == 0) {
                    pushback();
                    // and wait next signal from selector
                    return;
                }
                totalWrit+=numWrit;
            }
            request.reply(totalWrit);
            // keep signal
            channelAcc.up();
        }
    }

}
