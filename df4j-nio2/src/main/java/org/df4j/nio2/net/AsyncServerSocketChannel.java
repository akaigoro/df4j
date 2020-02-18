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
import org.df4j.core.port.OutScalars;
import org.df4j.core.util.Logger;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.*;


/**
 * generates flow of AsynchronousSocketChannel for server side
 * as a result of client connections accepted by the server
 *
 * Though it extends BasicBlock, it is effectively an Actor, as it is restarted from CompletionHandler.
 * Its single port is {@link AsyncServerSocketChannel#demands} with subscriptions.
 * The port is bounded, so incoming connections are accepted only when demands exist.
 *
 */
public class AsyncServerSocketChannel extends Actor implements CompletionHandler<AsynchronousSocketChannel, Void> {
    protected final Logger LOG = new Logger(this);
    protected volatile AsynchronousServerSocketChannel assc;
    public OutScalars<AsynchronousSocketChannel> demands = new OutScalars<>(this);

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

    public synchronized void close() {
        synchronized(this) {
            if (isCompleted()) {
                return;
            }
            AsynchronousServerSocketChannel asscLock = assc;
            assc = null;
            try {
                asscLock.close();
                onComplete();
            } catch (IOException e) {
                onError(e);
            }
        }
    }

    @Override
    protected final void runAction() throws Throwable {
        assc.accept(null, this);
        suspend(); // wait CompletionHandler
    }

    //====================== CompletionHandler's backend

    @Override
    public void completed(AsynchronousSocketChannel result, Void attachement) {
        LOG.info("AsyncServerSocketChannel: client accepted");
        demands.onNext(result);
        this.resume(); // allow  next assc.accpt()
    }

    /**
     * new client connection failed
     * TODO count failures, do not retry if many
     */
    @Override
    public void failed(Throwable exc, Void attachement) {
        LOG.info("AsyncServerSocketChannel: client rejected:"+exc);
        if (exc instanceof AsynchronousCloseException) {
            demands.onComplete();
            onComplete();
        } else {
            demands.onError(exc);
            onError(exc);
        }
    }
}
