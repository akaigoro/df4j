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

import org.df4j.core.dataflow.Actor;
import org.df4j.core.dataflow.Dataflow;
import org.df4j.core.port.InpSignal;
import org.df4j.core.util.Logger;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.*;


/**
 * generates flow of AsynchronousSocketChannel for server side
 * as a result of client connections accepted by the server
 *
 */
public abstract class AsyncServerSocketChannel extends Actor
        implements CompletionHandler<AsynchronousSocketChannel, Long>
{
    protected final Logger LOG = new Logger(this);
    protected volatile AsynchronousServerSocketChannel assc;
    /** limits the number of simultaneously existing connections */
    protected InpSignal allowedConnections = new InpSignal(this);
    protected long connSerialNum = 0;

    public AsyncServerSocketChannel(Dataflow dataflow, SocketAddress addr) throws IOException {
        super(dataflow);
        if (addr == null) {
            throw new NullPointerException();
        }
        AsynchronousChannelGroup group = AsynchronousChannelGroup.withThreadPool(dataflow.getExecutor());
        assc = AsynchronousServerSocketChannel.open(group);
        assc.bind(addr);
        LOG.info("AsyncServerSocketChannel("+addr+") created");
    }

    public synchronized void onComplete() {
        synchronized(this) {
            if (isCompleted()) {
                return;
            }
            AsynchronousServerSocketChannel asscLock = assc;
            assc = null;
            try {
                asscLock.close();
                super.onComplete();
            } catch (IOException e) {
                super.onError(e);
            }
        }
    }

    @Override
    protected final void runAction() throws Throwable {
        allowedConnections.acquire(); // got permission to accept client connection
        suspend(); // wait CompletionHandler
        assc.accept(connSerialNum++, this);
    }

    protected abstract void onAccept(AsynchronousSocketChannel asc, Long connSerialNum);

    //====================== CompletionHandler's backend

    @Override
    public void completed(AsynchronousSocketChannel asc, Long connSerialNum) {
        try {
            LOG.info("AsyncServerSocketChannel: client "+ connSerialNum+" accepted");
            onAccept(asc, connSerialNum);
            this.resume(); // allow  next assc.accpt()
        } catch (Throwable t) {
            try {
                asc.close();
            } catch (IOException e) {
            }
            onError(t);
        }
    }

    /**
     * new client connection failed
     * TODO count failures, do not retry if many
     */
    @Override
    public void failed(Throwable exc, Long attachement) {
        LOG.info("AsyncServerSocketChannel: client rejected:"+exc);
        if (exc instanceof AsynchronousCloseException) {
            onComplete();
        } else {
            onError(exc);
        }
    }
}
