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
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;

import com.github.rfqu.df4j.core.CompletableFuture;
import com.github.rfqu.df4j.core.ListenableFuture;
import com.github.rfqu.df4j.nio.AsyncSocketChannel;
import com.github.rfqu.df4j.nio.SocketIORequest;

public class AsyncSocketChannel2 extends AsyncSocketChannel {
    protected volatile AsynchronousSocketChannel channel;
    protected final ConnectionCompleter connEvent=new ConnectionCompleter();
    protected final CompletableFuture<AsyncSocketChannel> closeEvent=new CompletableFuture<AsyncSocketChannel>();

    {
        reader = new ReaderQueue();
        writer = new WriterQueue();
    }
    
    /** starts connection process from client side 
     * @throws IOException */
    public void connect(SocketAddress addr) throws IOException {
        AsynchronousChannelGroup acg=AsyncChannelCroup.getCurrentACGroup();
        AsynchronousSocketChannel channel=AsynchronousSocketChannel.open(acg);
        channel.connect(addr, channel, connEvent);
    }
    
    public void setTcpNoDelay(boolean on) throws IOException {
        channel.setOption(StandardSocketOptions.TCP_NODELAY, on);
    }

    public ListenableFuture<AsyncSocketChannel> getConnEvent() {
        return connEvent;
    }

    @Override
    public ListenableFuture<AsyncSocketChannel> getCloseEvent() {
        return closeEvent;
    }
    
    public synchronized AsynchronousSocketChannel getChannel() {
        return channel;
    }

    public boolean isConnected() {
        return channel!=null;
    }

    // ================== StreamPort I/O interface 

    /** disallows subsequent posts of requests; already posted requests 
     * would be processed.
     * @throws IOException 
     */
    @Override
    public synchronized void close() throws IOException {
        if (isClosed()) return;
        closeEvent.post(this);
        channel.close();
    }

    //===================== inner classes
    
    /**
     * callback for connection completion
     * works both in client-side and server-side modes
     */
    class ConnectionCompleter extends CompletableFuture<AsyncSocketChannel>
         implements CompletionHandler<Void, AsynchronousSocketChannel>
    {
        // ------------- CompletionHandler's backend

        @Override
        public void completed(Void result, AsynchronousSocketChannel asc) {
            channel=asc;
            reader.resume();
            writer.resume();
            super.post(AsyncSocketChannel2.this);
        }

        /**
         * in server-side mode, channel==null
         */
        @Override
        public void failed(Throwable exc, AsynchronousSocketChannel channel) {
            super.postFailure(exc);
        } 
    };

    abstract class RequestQueue2 extends RequestQueue
      implements CompletionHandler<Integer, SocketIORequest<?>>
    {
        protected Semafor channelAcc = new Semafor(); // channel accessible
        protected SocketIORequest<?> currentRequest;

        public RequestQueue2(boolean isReader) {
            super(null, isReader); // immediate executor - act() method
        }

        public void resume() {
            channelAcc.up();
        }

        // ------------- CompletionHandler's backend

        @Override
        public void completed(Integer result, SocketIORequest<?> request) {
            currentRequest = null;
            channelAcc.up();
            request.post(result);
        }

        @Override
        public void failed(Throwable exc, SocketIORequest<?> request) {
            if (exc instanceof AsynchronousCloseException) {
                synchronized (AsyncSocketChannel2.this) {
                    if (!AsyncSocketChannel2.this.isClosed()) {
                        closeEvent.post(AsyncSocketChannel2.this);
                    }
                }
            }
            currentRequest = null;
            channelAcc.up(); // let subsequent requests fail
            request.postFailure(exc);
        }
    }
	
    class ReaderQueue extends RequestQueue2 {
        
        public ReaderQueue() {
            super(true);
        }

        //-------------------- Actor's backend

        @Override
        protected void act(SocketIORequest<?> request) throws Exception {
            if (isClosed()) {
                request.postFailure(new AsynchronousCloseException());
                return;
            }
            currentRequest=request;
            if (request.isTimed()) {
                channel.read(request.getBuffer(),
                       request.getTimeout(), TimeUnit.MILLISECONDS, request, this);
            } else {
                channel.read(request.getBuffer(), request, this);
            }
        }
    }
   	
    class WriterQueue extends RequestQueue2 {
        
        public WriterQueue() {
            super(false);
        }

        //-------------------- Actor's backend

        @Override
        protected void act(SocketIORequest<?> request) throws Exception {
            if (isClosed()) {
                request.postFailure(new AsynchronousCloseException());
                return;
            }
        	currentRequest=request;
            if (request.isTimed()) {
                channel.write(request.getBuffer(), request.getTimeout(), TimeUnit.MILLISECONDS,
                        request, this);
            } else {
                channel.write(request.getBuffer(), request, this);
            }
        }
    }
}
