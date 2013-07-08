/*
 * Copyright 2011-2013 by Alexei Kaigorodov
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
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import com.github.rfqu.df4j.core.CompletableFuture;
import com.github.rfqu.df4j.core.DataflowNode;
import com.github.rfqu.df4j.core.ListenableFuture;
import com.github.rfqu.df4j.nio.SelectorThread.SelectorListener;

/**
 * Asynchronously executes I/O socket requests using {@link java.nio.channels.Selector}.
 * Simplifies input-output, handling queues of I/O requests.
 * 
 * Internally, manages 2 actors: one for reading requests and one for writing requests.
 * After request is served, it is sent to the port denoted by <code>replyTo</code> parameter in
 * the read/write methods.
 */
public class AsyncSocketChannel1 extends AsyncSocketChannel implements SelectorListenerUser {
    private SelectorThread selectorThread;
    protected volatile SocketChannel socketChannel;
    protected final ConnectionCompleter connectionCompleter;
    protected final ConnectionFuture connEvent=new ConnectionFuture();
    protected final CompletableFuture<AsyncSocketChannel> closeEvent=new CompletableFuture<AsyncSocketChannel>();
    protected SelectorListener selectorListener;
    
    public AsyncSocketChannel1(SelectorThread selectorThread) {
        this.selectorThread=selectorThread;
        reader = new RequestQueue1(selectorThread, true);
        writer = new RequestQueue1(selectorThread, false);
        connectionCompleter=new ConnectionCompleter(selectorThread);
    }

    public AsyncSocketChannel1() {
        this(SelectorThread.getCurrentSelectorThread());
    }

    /** for client-side connections
     */
    public ListenableFuture<AsyncSocketChannel> connect(final SocketAddress addr) throws IOException {
        // Create a non-blocking socket channel
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        // try to connect
        boolean connected = socketChannel.connect(addr);
        if (connected) {
            connEvent.postSocketChannel(socketChannel);
        } else {
        	// connection delayed
            connectionCompleter.connectSignal.up();
        }
        return connEvent;
    }
    
    public void setTcpNoDelay(boolean on) throws IOException {
//  TODO      socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, on);
    }

    public ListenableFuture<AsyncSocketChannel> getConnEvent() {
        return connEvent;
    }

    @Override
    public ListenableFuture<AsyncSocketChannel> getCloseEvent() {
        return closeEvent;
    }

    @Override
    public Selector getSelector() {
        return selectorThread.selector;
    }

    @Override
    public SelectableChannel getChannel() {
        return socketChannel;
    }
    
    // ================== StreamPort I/O interface 

    @Override
    public void close(){
        try {
			closeEvent.post(this);
		} catch (IllegalStateException ok) {
		}
		try {
            socketChannel.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    //===================== inner classes
    
    class ConnectionCompleter extends DataflowNode {
        Semafor connectSignal=new Semafor();

        public ConnectionCompleter(SelectorThread selectorThread) {
            super(selectorThread);
        }

        @Override
        protected void act() {
            boolean connected;
            try {
                connected = socketChannel.finishConnect();
                if (connected) {
                    connEvent.postSocketChannel(socketChannel);
                } else {
                    selectorListener.interestOn(SelectionKey.OP_CONNECT, connectSignal);
                }
            } catch (ClosedChannelException e) {
                closeEvent.post(AsyncSocketChannel1.this);
                connEvent.postFailure(e);
            } catch (IOException e) {
                connEvent.postFailure(e);
            }
        }
        
    }

    /**
     * callback for connection completion
     * works both in client-side and server-side modes
     */
    class ConnectionFuture extends CompletableFuture<AsyncSocketChannel> {
        public void postSocketChannel(SocketChannel channel) {
            try {
                channel.configureBlocking(false);
                channel.socket().setTcpNoDelay(true);
                socketChannel = channel;
                selectorListener=selectorThread.new SelectorListener(AsyncSocketChannel1.this);
                reader.resume();
                writer.resume();
                super.post(AsyncSocketChannel1.this);
            } catch (IOException e) {
                super.postFailure(e);
            }
        }
    }
    
    class RequestQueue1 extends RequestQueue {
        
        private final int keyBit;

        public RequestQueue1(SelectorThread selectorThread, boolean isReader) {
            super(selectorThread, isReader);
            if (isReader) {
                keyBit = SelectionKey.OP_READ;
            } else {
                keyBit = SelectionKey.OP_WRITE;
            }
        }

        //-------------------- Actor's backend

        @Override
        protected void act(SocketIORequest<?> request) {
            if (isClosed()) {
                request.postFailure(new AsynchronousCloseException());
                return;
            }
            currentRequest=request;
            /* TODO timed
            if (request.isTimed()) {
                socketChannel.read(request.getBuffer(),
                       request.getTimeout(), TimeUnit.MILLISECONDS, request, this);
            } else {
                socketChannel.read(request.getBuffer(), request, this);
            }
            */
            int nb;
            try {
                try {
                    ByteBuffer buffer = currentRequest.getBuffer();
                    if (isReader) {
                        nb = socketChannel.read(buffer);
                    } else {
                        nb = socketChannel.write(buffer);
                    }
                    if (nb != 0) {
                        currentRequest.post(nb);
                        channelAcc.up();
                    } else {
                        selectorListener.interestOn(keyBit, channelAcc);
                        input.pushback();
                    }
                } catch (ClosedChannelException e) {
                    AsyncSocketChannel1.this.close();
                    currentRequest.postFailure(e);
                }
            } catch (IOException e) {
                currentRequest.postFailure(e);
            }
        }
    }
    
}
