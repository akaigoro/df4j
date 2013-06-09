/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.nio;

import java.io.IOException;
import java.net.SocketAddress;

import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.core.CallbackPromise;

/**
 * Wrapper over {@link java.nio.channels.ServerSocketChannel} in non-blocking mode.
 * Simplifies input-output, handling queues of accept requests.
 */
public abstract class AsyncServerSocketChannel {
    protected Callback<AsyncSocketChannel> acceptor;
    /** how many connections may be accepted */
    protected int maxConn = 0;
    protected SocketAddress addr;
    protected CallbackPromise<SocketAddress> closeEvent = new CallbackPromise<SocketAddress>();

    public AsyncServerSocketChannel(SocketAddress addr, Callback<AsyncSocketChannel> acceptor) throws IOException {
        this.addr = addr;
        this.acceptor=acceptor;
    }

    /**
     * initiates acceptance process when the channel is free
     * 
     * @param acceptor
     */
    abstract public void up(int delta);

    public void up() {
        up(1);
    }

    public <R extends Callback<SocketAddress>> R addCloseListener(R listener) {
        closeEvent.addListener(listener);
        return listener;
    }

//    @Override
    abstract public void close();

    abstract public boolean isClosed();

}
