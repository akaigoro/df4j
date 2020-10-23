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
import org.df4j.core.actor.AsyncProc;
import org.df4j.core.port.InpFlood;
import org.df4j.core.port.InpSignal;
import org.df4j.core.util.LoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.*;

/**
 * Handles client's requests to connect.
 * Generates flow of AsynchronousSocketChannel for server side
 *
 * Though it is an input port, it does not require connection to other actor.
 * use {@link #connect(SocketAddress, int)} instead.
 *
 */
public class ServerSocketPort extends InpFlood<Connection> {
    ServerAcceptor asyncChannel;

    /**
     * @param parent {@link AsyncProc} to which this port belongs
     */
    public ServerSocketPort(AsyncProc parent) {
        super(parent);
    }

    public void connect(SocketAddress address, int connCount) throws IOException {
        asyncChannel = new ServerAcceptor(transition.getDataflow(), address);
        asyncChannel.start();
        asyncChannel.allowedConnections.release(connCount);
    }

    @Override
    public synchronized void cancel() {
        if (asyncChannel != null) {
            asyncChannel.complete();
            asyncChannel = null;
        }
        super.cancel();
    }

    class ServerAcceptor extends Actor implements CompletionHandler<AsynchronousSocketChannel, Void> {
        protected final Logger logger = LoggerFactory.getLogger(this);
        protected volatile AsynchronousServerSocketChannel assc;
        /**
         * limits the number of simultaneously existing connections - plays the role of backpressure
         */
        public InpSignal allowedConnections = new InpSignal(this);
        private Port serialAccess = new Port(this, true);

        public ServerAcceptor(ActorGroup actorGroup, SocketAddress addr) throws IOException {
            super(actorGroup);
            if (addr == null) {
                throw new NullPointerException();
            }
            AsynchronousChannelGroup group = AsynchronousChannelGroup.withThreadPool(actorGroup.getExecutor());
            assc = AsynchronousServerSocketChannel.open(group);
            assc.bind(addr);
            logger.info("AsyncServerSocketChannel(" + addr + ") created");
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
            serialAccess.block(); // wait for CompletionHandler to resume
            assc.accept(null, this);
        }

        //====================== CompletionHandler's backend

        @Override
        public void completed(AsynchronousSocketChannel asc, Void attachment) {
            try {
                serialAccess.unblock(); // allow  next assc.accpt()
                Connection serverConn = new Connection(asc, allowedConnections);
                ServerSocketPort.this.onNext(serverConn);
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
            logger.info("AsyncServerSocketChannel: client rejected:" + exc);
            if (exc instanceof AsynchronousCloseException) {
                ServerSocketPort.this.onComplete();
            } else {
                ServerSocketPort.this.onError(exc);
            }
        }
    }
}