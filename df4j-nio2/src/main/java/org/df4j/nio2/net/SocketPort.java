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

import org.df4j.core.actor.Actor;
import org.df4j.core.actor.AsyncProc;
import org.df4j.core.actor.Dataflow;
import org.df4j.core.port.InpFlood;
import org.df4j.protocol.OutMessagePort;
import org.df4j.core.util.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

/**
 * Wrapper over {@link AsynchronousSocketChannel}.
 * Simplfies input-output, handling queues of I/O requests.
 */
public class SocketPort extends InpFlood<ByteBuffer> implements OutMessagePort<ByteBuffer> {
    protected final Logger LOG = new Logger(this);

	/** read requests queue */
	private Reader reader;
	/** write requests queue */
    private Writer writer;

    protected volatile Connection channel;

    public String name;

    public SocketPort(AsyncProc parent) {
        super(parent);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void connect(Connection channel) {
        Dataflow dataflow = getDataflow();
        this.channel = channel;
        reader = new Reader(dataflow, this);
        writer = new Writer(dataflow, reader.input);
        reader.start();
        writer.start();
        LOG.info(name + " started");
    }

    public void connect(AsynchronousSocketChannel assc) {
        connect(new Connection(assc));
    }

    public void read(ByteBuffer buf) {
        reader.input.onNext(buf);
    }

    public void send(ByteBuffer buf) {
        writer.input.onNext(buf);
    }

    /**
     *
     * @throws IOException
     *          If an I/O error occurs
     */
    public synchronized void close() throws IOException {
        channel.close();
    }

    public synchronized boolean isClosed() {
        return channel==null;
    }

    @Override
    public String toString() {
        if (name == null) {
            return super.toString();
        } else {
            return name;
        }
    }

    //===================== inner classes

    /**
     * an actor with delayed restart of the action
     */
    protected abstract class IOExecutor extends Actor implements CompletionHandler<Integer, ByteBuffer> {
        protected final Logger LOG = new Logger(this);

        private final String io;
        protected final InpFlood<ByteBuffer> input = new InpFlood<>(this);
        protected final OutMessagePort<ByteBuffer> output;
        private Port serialAccess = new Port(this, true);

        long timeout=0;

        public IOExecutor(Dataflow dataflow, String io, OutMessagePort<ByteBuffer> output) {
            super(dataflow);
            this.output = output;
            setDaemon(true);
            this.io = io;
        }

        @Override
        protected void fire() {
            run();
        }
//-------------------- dataflow backend

        protected abstract void doIO(ByteBuffer buffer);

        protected abstract void doIO(ByteBuffer buffer, long timeout);

        @Override
        protected void runAction() throws CompletionException {
  //          LOG.info("conn "+ name+ ": " + io + " started");
            if (input.isCompleted()) {
                output.onError(input.getCompletionException());
                return;
            } else if (channel == null) {
                output.onComplete();
                return;
            }
            ByteBuffer buffer = input.remove();
            serialAccess.block(); // wait CompletionHandler to invoke resume()
            if (timeout > 0) {
                doIO(buffer, timeout);
            } else {
                doIO(buffer);
            }
        }

        // ------------- CompletionHandler backend

        // IO excange finished
        public void completed(Integer result, ByteBuffer buffer) {
  //          LOG.info("conn "+ name+": "+io+" completed "+result);
            if (result==-1) {
                output.onComplete();
            } else {
                output.onNext(buffer);
                // start next IO excange only after this reading is finished,
                // to keep buffer ordering
                serialAccess.unblock();
            }
        }

        public void failed(Throwable exc, ByteBuffer attach) {
 //           LOG.info("conn "+ name+": "+io+" failed "+exc);
            if (exc instanceof AsynchronousCloseException) {
                this.complete();
            } else {
                output.onError(exc);
            }
        }
    }

    /**
     * callback for connection completion
     * works both in client-side and server-side modes
     */
    
    protected class Reader extends IOExecutor {

        public Reader(Dataflow dataflow, OutMessagePort<ByteBuffer> output) {
            super(dataflow, "reader", output);
        }

        protected void doIO(ByteBuffer buffer) {
            buffer.clear();
            channel.getChannel().read(buffer, buffer, this);
        }

        @Override
        protected void doIO(ByteBuffer buffer, long timeout) {
            channel.getChannel().read(buffer, timeout, TimeUnit.MILLISECONDS, buffer, this);
        }
    }

    protected class Writer extends IOExecutor {

        public Writer(Dataflow dataflow, OutMessagePort<ByteBuffer> output) {
            super(dataflow, "writer", output);
        }

        protected void doIO(ByteBuffer buffer) {
            channel.getChannel().write(buffer, buffer, this);
        }

        @Override
        protected void doIO(ByteBuffer buffer, long timeout) {
            channel.getChannel().write(buffer, timeout, TimeUnit.MILLISECONDS, buffer, this);
        }
    }

}
