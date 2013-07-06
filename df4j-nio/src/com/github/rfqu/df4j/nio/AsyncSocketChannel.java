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

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousCloseException;
import java.util.concurrent.Executor;

import com.github.rfqu.df4j.core.Actor;
import com.github.rfqu.df4j.core.ListenableFuture;
import com.github.rfqu.df4j.ext.ImmediateExecutor;

/**
 * Wrapper over {@link AsynchronousSocketChannel}.
 * Simplifies input-output, handling queues of I/O requests.
 * 
 * For client-side connections, instatntiate and call connect(addr).
 * For server-side connections, obtain new instances via AsyncServerSocketChannel.accept().
 *  
 * Internally, manages 2 input queues: one for reading requests and one for writing requests.
 * After request is served, it is sent to the port denoted by <code>replyTo</code>
 * property in the request.
 * 
 * IO requests can be posted immediately, but will be executed
 * only after connection completes.
 * If interested in the moment when connection is established,
 * add a listener to connEvent.
 */
public abstract class AsyncSocketChannel
    implements Closeable
{
	/** read requests queue */
	protected RequestQueue reader;
	/** write requests queue */
	protected RequestQueue writer;

    /**
     * for client-side socket
     * Starts connection to a server. IO requests can be queued immediately,
     * but will be executed only after connection completes.
     * If interested in the moment when connection is established, add a
     * listener to the returned ListenableFuture.
     * @return 
     * 
     * @throws IOException
     */
    public abstract ListenableFuture<AsyncSocketChannel> connect(SocketAddress addr) throws IOException;
    
    /** signals connection completion */
    public abstract ListenableFuture<AsyncSocketChannel> getConnEvent();

    /** signals connection closing */
    public abstract ListenableFuture<AsyncSocketChannel> getCloseEvent();

    public boolean isConnected() {
        return getConnEvent().isDone();
    }

    public boolean isClosed() {
        return getCloseEvent().isDone();
    }

    public abstract void close() throws IOException;

    // ================== conventional I/O interface

	public void write(SocketIORequest<?> request) {
		request.prepareWrite();
		writer.post(request);
	}

	public void write(SocketIORequest<?> request,	long timeout) {
		request.prepareWrite(timeout);
		writer.post(request);
	}

	public void read(SocketIORequest<?> request) {
		request.prepareRead();
		reader.post(request);
	}

	public  void read(SocketIORequest<?> request, long timeout) {
		request.prepareRead(timeout);
		reader.post(request);
	}

    public abstract class RequestQueue extends Actor<SocketIORequest<?>> {
        protected Semafor channelAcc = new Semafor(); // channel accessible
        protected boolean isReader;
        protected SocketIORequest<?> currentRequest;

        public RequestQueue(Executor executor, boolean isReader) {
            super(executor);
            this.isReader=isReader;
        }

        
        @Override
        public synchronized void post(SocketIORequest<?> request) {
            if (isClosed()) {
                request.postFailure(new AsynchronousCloseException());
                return;
            }
            if (!request.getBuffer().hasRemaining()) {
                request.postFailure(new IllegalArgumentException());
                return;
            }
            super.post(request);
        }

        public void resume() {
            channelAcc.up();
        }

        // ------------- CompletionHandler's backend

        public void completed(Integer result, SocketIORequest<?> request) {
            currentRequest = null;
            channelAcc.up();
            request.post(result);
        }

        public void failed(Throwable exc, SocketIORequest<?> request) {
            if (exc instanceof AsynchronousCloseException) {
                synchronized (AsyncSocketChannel.this) {
                    try {
                        AsyncSocketChannel.this.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            currentRequest = null;
            channelAcc.up(); // let subsequent requests fail
            request.postFailure(exc);
        }
    }

}
