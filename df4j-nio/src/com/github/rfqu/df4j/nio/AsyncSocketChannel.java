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

import java.util.concurrent.Executor;

import com.github.rfqu.df4j.core.Actor;
import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.core.ListenableFuture;
import com.github.rfqu.df4j.core.DataflowVariable;
import com.github.rfqu.df4j.core.StreamPort;

/**
 * Asynchronously executes I/O socket requests using {@link java.nio.channels.Selector}.
 * Simplifies input-output, handling queues of I/O requests.
 * 
 * Internally, manages 2 actors: one for reading requests and one for writing requests.
 * After request is served, it is sent to the port denoted by <code>replyTo</code> parameter in
 * the read/write methods.
 */
public abstract class AsyncSocketChannel
    implements StreamPort<SocketIORequest<?>>
{
	/** for client-side socket: signals connection completion */
	protected final ListenableFuture<AsyncSocketChannel> connEvent = new ListenableFuture<AsyncSocketChannel>();
	/** read requests queue */
	protected RequestQueue reader;
	/** write requests queue */
	protected RequestQueue writer;
	/** closes channel */
	protected Completer completer = new Completer();
	protected volatile boolean closed = false;

	public <R extends Callback<AsyncSocketChannel>> R addConnListener(R listener) {
		connEvent.addListener(listener);
		return listener;
	}

	public boolean isClosed() {
		return closed;
	}

    public ListenableFuture<AsyncSocketChannel> getConnEvent() {
        return connEvent;
    }

	// ================== StreamPort I/O interface

    @Override
	public void post(SocketIORequest<?> request) {
		(request.isReadOp() ? reader : writer).post(request);
	}

	// ================== conventional I/O interface

	public <R extends SocketIORequest<R>> void write(R request) {
		request.prepareWrite();
		post(request);
	}

	public <R extends SocketIORequest<R>> void write(R request,	long timeout) {
		request.prepareWrite(timeout);
		post(request);
	}

	public <R extends SocketIORequest<R>> void read(R request) {
		request.prepareRead();
		post(request);
	}

	public <R extends SocketIORequest<R>> void read(R request, long timeout) {
		request.prepareRead(timeout);
		post(request);
	}

    public abstract class RequestQueue extends Actor<SocketIORequest<?>> {

        public RequestQueue(Executor executor) {
            super(executor);
        }

        public abstract void resume();
        
        @Override
        public synchronized void post(SocketIORequest<?> request) {
            if (!request.getBuffer().hasRemaining()) {
                request.postFailure(new IllegalArgumentException());
                return;
            }
            super.post(request);
        }
 	}

	/**
	 * closes underlying SocketChannel after all requests has been processed.
	 */
	public class Completer extends DataflowVariable {
		private final Semafor readerFinished = new Semafor();
		private final Semafor writerFinished = new Semafor();

		@Override
		protected void act() {
		    AsyncSocketChannel.this.close();
		}

        public Semafor getReaderFinished() {
            return readerFinished;
        }

        public Semafor getWriterFinished() {
            return writerFinished;
        }

	}

}
