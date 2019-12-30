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

import org.df4j.core.communicator.AsyncSemaphore;
import org.df4j.core.dataflow.BasicBlock;
import org.df4j.core.dataflow.Dataflow;
import org.df4j.core.port.InpFlow;
import org.df4j.core.port.OutFlow;
import org.df4j.core.util.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;

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
public class BaseConnection extends Dataflow {
    protected static final Logger LOG = Logger.getLogger(BaseConnection.class.getName());

    private AsyncSemaphore backPort;

	/** read requests queue */
	public final Reader reader = new Reader();
	/** write requests queue */
	public final Writer writer = new Writer();

    protected volatile AsynchronousSocketChannel channel;

    public String name;

    public BaseConnection(String name, AsyncSemaphore backPort) {
        this.name = name;
        this.backPort = backPort;
    }

    public void setChannel(AsynchronousSocketChannel channel) {
        LOG.info("conn "+name+": init()");
        this.channel=channel;
        reader.awake();
        writer.awake();
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
            backPort.release();
        }
    }

    public synchronized boolean isClosed() {
        return channel==null;
    }

    //===================== inner classes

    /**
     * an actor with delayed restart of the action
     */
    public abstract class BuffProcessor extends BasicBlock
            implements CompletionHandler<Integer, ByteBuffer>
    {
        protected final Logger LOG = Logger.getLogger(getClass().getName());

        public final InpFlow<ByteBuffer> input = new InpFlow<>(this);
        public final OutFlow<ByteBuffer> output = new OutFlow<>(this);

        {
            LOG.info("BuffProcessor "+getClass().getName()+" "+name+" created");
        }

        long timeout=0;

        protected BuffProcessor() {
            super(BaseConnection.this);
        }

        //-------------------- dataflow backend

        @Override
        protected void runAction() {
            ByteBuffer buffer = input.current();
            if (input.isCompleted()) {
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
                this.awake();
            }
        }

        public void failed(Throwable exc, ByteBuffer attach) {
            LOG.finest("conn "+ name+": read() failed "+exc);
            if (exc instanceof AsynchronousCloseException) {
                close();
            } else {
                this.awake(); // let subsequent requests fail
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
