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
package org.df4j.nio2.file;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.dataflow.Dataflow;
import org.df4j.core.port.InpFlow;
import org.df4j.core.port.OutFlow;
import org.df4j.core.util.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;

/**
 * Wrapper over {@link AsynchronousFileChannel}.
 * Simplifies input-output, handling queues of {@link ByteBuffer}s.
 */
public abstract class AsyncFileChannel extends Actor implements CompletionHandler<Integer, ByteBuffer> {
    protected final Logger LOG = new Logger(this, Level.INFO);
    protected volatile AsynchronousFileChannel channel;
    /** input queue of empty buffers */
    public final InpFlow<ByteBuffer> input;
    /** output queue of filled buffers */
    public final OutFlow<ByteBuffer> output;
    protected volatile long filePosition = 0;

    public AsyncFileChannel(Dataflow dataflow, AsynchronousFileChannel channel, int capacity) {
        super(dataflow);
        this.channel=channel;
        input = new InpFlow<>(this, capacity);
        output = new OutFlow<>(this, capacity);
    }

    /** disallows subsequent posts of requests; already posted requests
     * would be processed.
     */
    public synchronized void close() {
        AsynchronousFileChannel locchannel;
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
    }

    protected abstract void doIO(ByteBuffer buffer);

    //-------------------- dataflow backend

    /**
     *  All conditiona are met, start IO operation
     */
    @Override
    protected void runAction() throws CompletionException {
        if (!input.isCompleted()) {
            ByteBuffer buffer = input.remove();
            suspend(); // before doIO, as it can call resume
            doIO(buffer);
        } else {
            try {
                channel.close();
                Throwable completionException = input.getCompletionException();
                if (completionException == null) {
                    output.onComplete();
                } else {
                    output.onError(completionException);
                }
                complete();
            } catch (IOException e) {
                output.onError(e);
                completeExceptionally(e);
            }
        }
    }

    // ------------- CompletionHandler backend

    // IO operation finished
    public void completed(Integer result, ByteBuffer buffer) {
        //          LOG.info("conn "+ name+": "+io+" completed "+result);
        if (result==-1) {
            output.onComplete();
            close();
        } else {
            filePosition += result;
            buffer.flip();
            output.onNext(buffer);
            // start next IO excange only after this reading is finished,
            // to keep buffer ordering
            this.resume();
        }
    }

    public void failed(Throwable exc, ByteBuffer attach) {
        //           LOG.info("conn "+ name+": "+io+" failed "+exc);
        if (exc instanceof AsynchronousCloseException) {
            close();
        } else {
            this.resume(); // let subsequent requests fail
            output.onError(exc);
        }
    }
}
