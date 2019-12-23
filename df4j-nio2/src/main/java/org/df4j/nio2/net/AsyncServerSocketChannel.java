/*
 * Copyright 2013 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.nio2.net;

import org.df4j.core.dataflow.BasicBlock;
import org.df4j.core.dataflow.Dataflow;
import org.df4j.core.communicator.AsyncSemaphore;
import org.df4j.core.port.InpSignalFlow;
import org.df4j.core.util.Logger;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 * Accepts incoming connections.
 *
 * Though it extends BasicBlock, it is effectively an Actor, it is restarted from CompletionHandler.
 *
 *  Total number of active connections is limited by {@link AsyncSemaphore} {@link AsyncServerSocketChannel#allowedConnections}.
 *  Subclasses must provide required number of permits.
 *
 */
public abstract class AsyncServerSocketChannel extends BasicBlock
        implements CompletionHandler<AsynchronousSocketChannel, Void>
{
    protected InpSignalFlow listener = new InpSignalFlow(this);
    protected final Logger LOG = Logger.getLogger(AsyncServerSocketChannel.class.getName());
    protected volatile AsynchronousServerSocketChannel assc;
    protected AsyncSemaphore allowedConnections = new AsyncSemaphore();

    public AsyncServerSocketChannel(SocketAddress addr) throws IOException {
        super(new Dataflow());
        if (addr == null) {
            throw new NullPointerException();
        }
        assc = AsynchronousServerSocketChannel.open();
        assc.bind(addr);
        LOG.config("AsyncServerSocketChannel("+addr+") created");
        allowedConnections.subscribe(listener);
        this.awake();
    }

    public synchronized void close() {
        if (assc == null) {
            return;
        }
        AsynchronousServerSocketChannel asscLock = assc;
        assc = null;
        try {
            asscLock.close();
            stop();
        } catch (IOException e) {
            stop(e);
        }
    }

    @Override
    protected final void runAction() throws Throwable {
        assc.accept(null, this);
    }

    //====================== CompletionHandler's backend

    @Override
    public void completed(AsynchronousSocketChannel result, Void attachement) {
        onNext(result);
        allowedConnections.subscribe(listener);
        this.awake(); // allow  next assc.accpt()
    }

    /**
     * new client connection failed
     * TODO count failures, do not retry if many
     */
    @Override
    public void failed(Throwable exc, Void attachement) {
        onError(exc);
        if (exc instanceof AsynchronousCloseException) {
            // channel closed.
            close();
        } else {
            this.awake(); // TODO do we really want to allow next call to assc.accept() after failure?
        }
    }

    protected abstract void onNext(AsynchronousSocketChannel asc);

    protected abstract void onError(Throwable exc);
}
