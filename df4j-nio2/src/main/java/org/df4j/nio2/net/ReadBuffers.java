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

import org.df4j.core.dataflow.Actor;
import org.df4j.core.dataflow.AsyncProc;
import org.df4j.core.dataflow.Dataflow;
import org.df4j.core.port.InpFlood;
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
 * Simplifies input-output, handling queues of I/O requests.
 */
public class ReadBuffers extends InpFlood<ByteBuffer> {
    protected final Logger LOG = new Logger(this);
    private final Dataflow dataflow;

	/** read requests queue */
	public final Reader reader;
	/** write requests queue */
	public final Writer writer;

    protected volatile AsynchronousSocketChannel channel;

    public String name;

    public ReadBuffers(AsyncProc parent, AsynchronousSocketChannel channel) {
        super(parent);
        this.dataflow=parent.getDataflow();
        this.channel=channel;
        reader = new Reader(dataflow);
        writer = new Writer(dataflow, reader.input);
        reader.start();
        writer.start();
    }

    public void setName(String name) {
        this.name = name;
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
    public abstract class IOExecutor extends Actor implements CompletionHandler<Integer, ByteBuffer> {
        protected final Logger LOG = new Logger(this);

        final String io;
        public final InpFlood<ByteBuffer> input = new InpFlood<>(this);
        public final InpFlood<ByteBuffer> output;

        long timeout=0;

        public IOExecutor(Dataflow dataflow, String io, InpFlood<ByteBuffer> output) {
            super(dataflow);
            this.output = output;
            setDaemon(true);
            this.io = io;
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
            suspend(); // wait CompletionHandler to invoke resume()
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
                this.resume();
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
    
    public class Reader extends IOExecutor {

        public Reader(Dataflow dataflow) {
            super(dataflow, "reader", ReadBuffers.this);
        }

        protected void doIO(ByteBuffer buffer) {
            buffer.clear();
            channel.read(buffer, buffer, this);
        }

        @Override
        protected void doIO(ByteBuffer buffer, long timeout) {
            channel.read(buffer, timeout, TimeUnit.MILLISECONDS, buffer, this);
        }
    }
    
    public class Writer extends IOExecutor {

        public Writer(Dataflow dataflow, InpFlood<ByteBuffer> output) {
            super(dataflow, "writer", output);
        }

        protected void doIO(ByteBuffer buffer) {
            channel.write(buffer, buffer, this);
        }

        @Override
        protected void doIO(ByteBuffer buffer, long timeout) {
            channel.write(buffer, timeout, TimeUnit.MILLISECONDS, buffer, this);
        }
    }

}
