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
import com.github.rfqu.df4j.core.DataflowVariable;
import com.github.rfqu.df4j.core.Link;
import com.github.rfqu.df4j.core.Port;
import com.github.rfqu.df4j.core.Promise;
import com.github.rfqu.df4j.core.StreamPort;
import com.github.rfqu.df4j.ext.ActorVariableDLQ;

/**
 * Asynchronously executes I/O socket requests using {@link java.nio.channels.Selector}.
 * Simplifies input-output, handling queues of I/O requests.
 * 
 * Internally, manages 2 actors: one for reading requests and one for writing requests.
 * After request is served, it is sent to the port denoted by <code>replyTo</code> parameter in
 * the read/write methods.
 */
public class AsyncSocketChannel extends Link implements
		StreamPort<SocketIORequest<?>> {
	private SelectorThread currentSelectorThread = SelectorThread
			.getCurrentSelectorThread();
	protected volatile SocketChannel socketChannel;
	private MyListener selectorListener = new MyListener();
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
		currentSelectorThread.register(channel, SelectionKey.OP_READ
				| SelectionKey.OP_READ, selectorListener);
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
	public AsyncSocketChannel(SocketAddress addr) throws IOException {
		// Create a non-blocking socket channel
		SocketChannel channel = SocketChannel.open();
		channel.configureBlocking(false);

		boolean connected = channel.connect(addr);
		if (connected) {
			init(channel);
		} else {
			// Kick off connection establishment
			currentSelectorThread.register(channel, SelectionKey.OP_CONNECT,
					selectorListener);
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
		if (closed) {
			request.failed(new ClosedChannelException());
			return;
		}
		(request.isReadOp() ? reader : writer).send(request);
	}

	/**
	 * disallows subsequent posts of requests; already posted requests would be
	 * processed.
	 */
	@Override
	public void close() {
		closed = true;
		reader.close();
		writer.close();
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

	/**
	 * wrapped in a separate class in order not to expose callback methods
	 */
	class MyListener implements SocketEventListener {
		/**
		 * callback method for successful connection in client-side mode
		 */
		@Override
		public void connect(SelectionKey key) {
			try {
				SocketChannel channel = (SocketChannel) key.channel();
				channel.finishConnect();
				init(channel);
			} catch (IOException e) {
				e.printStackTrace();
				AsyncSocketChannel.this.close(); // TODO send failure
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
		public void close() {
			AsyncSocketChannel.this.close();
		}
	}

	abstract class RequestQueue extends ActorVariableDLQ<SocketIORequest<?>> {
		protected Semafor channelAcc = new Semafor(); // channel accessible
		protected SocketIORequest<?> currentRequest;

		protected void resume() {
			channelAcc.up();
		}
	}

	class ReaderQueue extends RequestQueue {
		/**
		 * Both the request and channel are ready
		 */
		@Override
		protected void act(SocketIORequest<?> request) throws Exception {
			if (AsyncSocketChannel.this.closed) {
				request.failed(new AsynchronousCloseException());
				return;
			}
			ByteBuffer readBuffer = request.getBuffer();

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
				// no more date, wait next signal from selector,
				// then repeat attempt to read to the same buffer
				pushback();
			} else {
				request.reply(numRead);
				// continue reading to the next buffer
				channelAcc.up();
			}

		}

		@Override
		protected void complete() throws Exception {
			completer.readerFinished.up();
		}

	}

	class WriterQueue extends RequestQueue {
		/**
		 * Both the request and channel are ready
		 */
		@Override
		protected void act(SocketIORequest<?> request) throws Exception {
			if (AsyncSocketChannel.this.closed) {
				request.failed(new AsynchronousCloseException());
				return;
			}
			ByteBuffer writeBuffer = request.getBuffer();

			int totalWrit = 0;
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
					request.reply(-1); // TODO define how to signal end of
										// stream
					AsyncSocketChannel.this.close();
					return;
				}
				if (numWrit == 0) {
					pushback();
					// and wait next signal from selector
					return;
				}
				totalWrit += numWrit;
			}
			request.reply(totalWrit);
			// keep signal
			channelAcc.up();
		}

		@Override
		protected void complete() throws Exception {
			completer.writerFinished.up();
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
			try {
				socketChannel.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
