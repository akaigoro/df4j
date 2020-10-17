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
import org.df4j.core.port.OutFlood;
import org.df4j.core.util.Logger;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.*;

/**
 * generates flow of AsynchronousSocketChannel for server side
 * as a result of client connections accepted by the server
 */
public class AsyncServerSocketChannel extends Actor
        implements CompletionHandler<AsynchronousSocketChannel, Void>
{
    protected final Logger LOG = new Logger(this);
    protected volatile AsynchronousServerSocketChannel assc;
    /** limits the number of simultaneously existing connections - plays the role of backpressure */
    public InpSignal allowedConnections = new InpSignal(this);
    public OutFlood<AsyncSocketChannel> out = new OutFlood<>(this);

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

    public synchronized void whenComplete() {
        AsynchronousServerSocketChannel asscLock = assc;
        if (asscLock == null) {
            return;
        }
        assc = null;
        try {
            asscLock.close();
        } catch (IOException e) {
            super.completeExceptionally(e);
        }
    }

    @Override
    protected final void runAction() throws Throwable {
        allowedConnections.remove(); // use a permit to accept next client connection
        suspend(); // wait for CompletionHandler to resume
        assc.accept(null, this);
    }

    //====================== CompletionHandler's backend

    @Override
    public void completed(AsynchronousSocketChannel asc, Void attachment) {
        try {
            this.resume(); // allow  next assc.accpt()
            AsyncSocketChannel serverConn = new AsyncSocketChannel(getDataflow(), allowedConnections, asc);
            out.onNext(serverConn);
        } catch (Throwable t) {
            try {
                asc.close();
            } catch (IOException e) {
            }
            completeExceptionally(t);
        }
    }

    /**
     * new client connection failed
     * TODO count failures, do not retry if many
     */
    @Override
    public void failed(Throwable exc, Void attachement) {
        LOG.info("AsyncServerSocketChannel: client rejected:"+exc);
        if (exc instanceof AsynchronousCloseException) {
            out.onComplete();
        } else {
            out.onError(exc);
        }
    }
}
