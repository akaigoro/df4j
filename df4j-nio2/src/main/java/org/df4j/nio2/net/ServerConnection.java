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
package org.df4j.nio2.net;

import org.df4j.core.stream.StreamInput;
import org.df4j.core.stream.MulticastStreamOutput;
import org.df4j.core.scalar.ext.Action;
import org.df4j.core.scalar.ext.AsyncAction;
import org.df4j.core.util.Logger;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Wrapper over {@link AsynchronousSocketChannel}.
 * Simplifies input-output, handling queues of I/O requests.
 *
 * For server-side connections, instatntiate and send to {@link AsyncServerSocketChannel}.
 * For client-side connections, instantiate subclass {@link ClientConnection} and call connect(addr).
 *
 * Internally, manages 2 input queues: one for reading requests and one for writing requests.
 * After request is served, it is sent to the port denoted by <code>replyTo</code>
 * property in the request.
 *
 * IO requests can be posted immediately, but will be executed
 * only after connection completes.
 */
public class ServerConnection implements Subscriber<AsynchronousSocketChannel> {
    protected static final Logger LOG = Logger.getLogger(ServerConnection.class.getName());

    private final Consumer<ServerConnection> backPort;

	/** read requests queue */
	public final Reader reader;
	/** write requests queue */
	public final Writer writer;

    protected volatile AsynchronousSocketChannel channel;

    public String name;

    public ServerConnection(String name, Consumer<ServerConnection> backPort) {
        this.name = name;
        this.backPort = backPort;
        reader = new Reader();
        writer = new Writer();
        LOG.config(getClass().getName()+" "+name+" created");
    }

    public ServerConnection(String name) {
        this(name, null);
    }

    @Override
    public void onSubscribe(Subscription s) {

    }

    public void onNext(AsynchronousSocketChannel channel) {
        LOG.info("conn "+name+": init()");
        this.channel=channel;
        reader.start();
        writer.start();
    }

    public void onError(Throwable ex) {
        LOG.info("conn "+name+": postFailure()");
    }

    @Override
    public void onComplete() {

    }

    /** disallows subsequent posts of requests; already posted requests
     * would be processed.
     */
    public synchronized void close() {
        AsynchronousSocketChannel locchannel;
        synchronized (this) {
            locchannel = channel;
            channel=null;
        }
    	if (locchannel!=null) {
            try {
                locchannel.close();
            } catch (IOException e) {
            }
    	}
    	if (backPort != null) {
            backPort.accept(this);
        }
    }

    public synchronized boolean isClosed() {
        return channel==null;
    }

    //===================== inner classes

    /**
     * an actor with delayed restart of the action
     */
    public abstract class BuffProcessor extends AsyncAction
            implements CompletionHandler<Integer, ByteBuffer>
    {
        protected final Logger LOG = Logger.getLogger(getClass().getName());

        public final StreamInput<ByteBuffer> input = new StreamInput<ByteBuffer>(this);
        public final MulticastStreamOutput<ByteBuffer> output = new MulticastStreamOutput<>(this);

        {
            LOG.info(getClass().getName()+" "+name+" created");
        }

        long timeout=0;

        //-------------------- datafloe backend

        @Action
        protected void start_IO (ByteBuffer buffer) {
            if (input.isClosed()) {
                output.onComplete();
                output.onError(new AsynchronousCloseException());
                LOG.finest("conn "+ name+": input.isClosed()");
                return;
            }
            doIO(buffer);
        }

        // ------------- CompletionHandler backend

        // IO excange finished
        public void completed(Integer result, ByteBuffer buffer) {
            LOG.finest("conn "+ name+": read() completed "+result);
            if (result==-1) {
                output.onComplete();
                close();
            } else {
                buffer.flip();
                output.onNext(buffer);
                // start next IO excange only after this reading is finished,
                // to keep buffer ordering
                this.start();
            }
        }

        public void failed(Throwable exc, ByteBuffer attach) {
            LOG.finest("conn "+ name+": read() failed "+exc);
            if (exc instanceof AsynchronousCloseException) {
                close();
            } else {
                this.start(); // let subsequent requests fail
                output.onError(exc);
            }
        }

        protected abstract void doIO(ByteBuffer buffer);

    }

    /**
     * callback for connection completion
     * works both in client-side and server-side modes
     */
    
    public class Reader extends BuffProcessor {

        protected void doIO(ByteBuffer buffer) {
            LOG.info("conn "+name+": read() started");
            if (timeout>0) {
                channel.read(buffer, timeout, TimeUnit.MILLISECONDS, buffer, this);
            } else {
                channel.read(buffer, buffer, this);
            }
        }

    }
    
    public class Writer extends BuffProcessor {

        protected void doIO(ByteBuffer buffer) {
            LOG.finest("conn "+name+": write() started.");
            if (timeout>0) {
                channel.write(buffer, timeout, TimeUnit.MILLISECONDS, buffer, this);
            } else {
                channel.write(buffer, buffer, this);
            }
        }
    }

}
