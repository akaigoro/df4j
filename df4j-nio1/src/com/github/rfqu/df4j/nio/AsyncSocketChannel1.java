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
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import com.github.rfqu.df4j.core.CompletableFuture;
import com.github.rfqu.df4j.core.ListenableFuture;

/**
 * Asynchronously executes I/O socket requests using {@link java.nio.channels.Selector}.
 * Simplifies input-output, handling queues of I/O requests.
 * 
 * Internally, manages 2 actors: one for reading requests and one for writing requests.
 * After request is served, it is sent to the port denoted by <code>replyTo</code> parameter in
 * the read/write methods.
 */
public class AsyncSocketChannel1 extends AsyncSocketChannel {
    private SelectorThread selectorThread;
    protected volatile SocketChannel socketChannel;
    protected final ConnectionCompleter connEvent=new ConnectionCompleter();
    protected final CompletableFuture<AsyncSocketChannel> closeEvent=new CompletableFuture<AsyncSocketChannel>();
    protected SelectorListener selectorListener;//=new SelectorListener(); 
    
    {
        reader = new ReaderQueue();
        writer = new WriterQueue();
    }
    
    public AsyncSocketChannel1(SelectorThread selectorThread) {
        this.selectorThread=selectorThread;
    }

    public AsyncSocketChannel1() {
        this(SelectorThread.getCurrentSelectorThread());
    }

    public ListenableFuture<AsyncSocketChannel> connect(final SocketAddress addr) throws IOException {
        // Create a non-blocking socket channel
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        selectorListener=new SelectorListener(selectorThread.selector, socketChannel);
        // try to connect
        boolean connected = socketChannel.connect(addr);
        if (connected) {
            connEvent.postSocketChannel(socketChannel);
            return connEvent;
        }
        selectorThread.execute(new Runnable() {
            public void run() {
                try {
                    selectorListener.interestOn(SelectionKey.OP_CONNECT);
                } catch (ClosedChannelException e) {
                    closeEvent.post(AsyncSocketChannel1.this);
                    connEvent.postFailure(e);
                }
            }
        });
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
    
    // ================== StreamPort I/O interface 

    @Override
    public synchronized void close() throws IOException {
        if (isClosed()) return;
        closeEvent.post(this);
        socketChannel.close();
    }

    //===================== inner classes
    
    /**
     * callback for connection completion
     * works both in client-side and server-side modes
     */
    class ConnectionCompleter extends CompletableFuture<AsyncSocketChannel> {
        public void postSocketChannel(SocketChannel channel) {
            try {
                channel.configureBlocking(false);
                channel.socket().setTcpNoDelay(true);
                socketChannel = channel;
                if (selectorListener==null) {
                    selectorListener=new SelectorListener(selectorThread.selector, channel);
                }
                reader.resume();
                writer.resume();
                super.post(AsyncSocketChannel1.this);
            } catch (IOException e) {
                super.postFailure(e);
            }
        }
    }

    class ReaderQueue extends RequestQueue {
        
        public ReaderQueue() {
            super(selectorThread, true);
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
            doRead();
        }
        
        void doRead() {
            int nb;
            try {
                try {
                    nb = socketChannel.read(currentRequest.getBuffer());
                    if (nb != 0) {
                        selectorListener.interestOff(SelectionKey.OP_READ);
                        currentRequest.post(nb);
                        channelAcc.up();
                    } else {
                        selectorListener.interestOn(SelectionKey.OP_READ);
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
    
    class WriterQueue extends RequestQueue {
        
        public WriterQueue() {
            super(selectorThread, false);
        }

        //-------------------- Actor's backend

        @Override
        protected void act(SocketIORequest<?> request) throws Exception {
            if (isClosed()) {
                request.postFailure(new AsynchronousCloseException());
                return;
            }
            currentRequest=request;
            /* TODO timed
            if (request.isTimed()) {
                socketChannel.write(request.getBuffer(), request.getTimeout(), TimeUnit.MILLISECONDS,
                        request, this);
            } else {
                socketChannel.write(request.getBuffer(), request, this);
            }
            */
            doWrite();
        }

        public void doWrite() {
            int nb;
            try {
                try {
                    nb = socketChannel.write(currentRequest.getBuffer());
                    if (nb!=0) {
                        selectorListener.interestOff(SelectionKey.OP_WRITE);
                        currentRequest.post(nb);
                        channelAcc.up();
                    } else {
                        selectorListener.interestOn(SelectionKey.OP_WRITE);
                    }
                } catch (ClosedChannelException e) {
                    AsyncSocketChannel1.this.close();
                    currentRequest.postFailure(e);
                }
            } catch (ClosedChannelException e) {
                closeEvent.post(AsyncSocketChannel1.this);
                connEvent.postFailure(e);
            } catch (IOException e) {
                currentRequest.postFailure(e);
            }
        }
    }
    
    class SelectorListener extends SelectorEventListener {
        
        public SelectorListener(Selector selector, SocketChannel socketChannel) throws ClosedChannelException {
            super(selector, socketChannel);
        }

        /** called on selector thread
         * 
         */
        @Override
        public void run() {
            if (key.isConnectable()) {
                boolean connected;
                try {
                    connected = socketChannel.finishConnect();
                    if (connected) {
                        connEvent.postSocketChannel(socketChannel);
                        interestOff(SelectionKey.OP_CONNECT);
                    } else {
                        interestOn(SelectionKey.OP_CONNECT);
                    }
                } catch (ClosedChannelException e) {
                    closeEvent.post(AsyncSocketChannel1.this);
                    connEvent.postFailure(e);
                } catch (IOException e) {
                    connEvent.postFailure(e);
                }
            }
            if (key.isValid()&& key.isReadable()) {
                ((ReaderQueue)reader).doRead();
            }
            if (key.isValid()&& key.isWritable()) {
                ((WriterQueue)writer).doWrite();
            }
        }

        protected void stop() {
            SelectionKey key = socketChannel.keyFor(selectorThread.selector);
            if (key==null || !key.isValid()) {
                return;
            }
            key.cancel();
            socketChannel=null;
        }
    }

}
