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

import org.df4j.core.actor.Actor;
import org.df4j.core.actor.ActorGroup;
import org.df4j.core.port.InpSignal;
import org.df4j.core.port.OutFlood;
import org.df4j.core.util.LoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;


/**
 * generates flow of AsynchronousSocketChannel for server side
 * as a result of client connections accepted by the server
 *
 */
public class AsyncServerSocketChannel extends Actor
        implements CompletionHandler<AsynchronousSocketChannel, Long>
{
    private static final ExecutorService execService = ForkJoinPool.commonPool();
    private final Logger LOG = LoggerFactory. getLogger(this);
    private volatile AsynchronousServerSocketChannel assc;
    /** limits the number of simultaneously existing connections */
    private InpSignal allowedConnections = new InpSignal(this);
    public final OutFlood<Connection> out = new OutFlood<>(this);
    protected long connSerialNum = 0;

    public AsyncServerSocketChannel(ActorGroup dataflow, SocketAddress addr) throws IOException {
        super(dataflow);
        if (addr == null) {
            throw new NullPointerException();
        }
        AsynchronousChannelGroup group = AsynchronousChannelGroup.withThreadPool(execService);
        assc = AsynchronousServerSocketChannel.open(group);
        assc.bind(addr);
        LOG.info("AsyncServerSocketChannel("+addr+") created");
    }

    public void release(int count) {
        allowedConnections.release(count);
    }

    protected synchronized void whenComplete(Throwable ex) {
        AsynchronousServerSocketChannel asscLock = assc;
        assc = null;
        try {
            asscLock.close();
            super.complete(ex);
        } catch (IOException e) {
            super.complete(e);
        }
    }

    public void onComplete(Throwable ex) {
        complete(ex);
    }

    @Override
    protected final void runAction() throws Throwable {
        allowedConnections.remove(); // use up a permit to accept next client connection
        suspend(); // wait for CompletionHandler to resume
        assc.accept(connSerialNum++, this);
    }

    //====================== CompletionHandler's backend

    @Override
    public void completed(AsynchronousSocketChannel asc, Long connSerialNum) {
        try {
            LOG.info("AsyncServerSocketChannel: client "+ connSerialNum+" accepted");
            Connection conn = new Connection(asc, connSerialNum, allowedConnections);
            out.onNext(conn);
            this.resume(); // allow  next assc.accpt()
        } catch (Throwable t) {
            try {
                asc.close();
            } catch (IOException e) {
            }
            complete(t);
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
            complete();
        } else {
            complete(exc);
        }
    }
}
