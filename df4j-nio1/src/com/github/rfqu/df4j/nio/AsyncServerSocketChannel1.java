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
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import com.github.rfqu.df4j.core.Actor;
import com.github.rfqu.df4j.core.ListenableFuture;
import com.github.rfqu.df4j.nio.SelectorThread.SelectorListener;

/**
 * Wrapper over {@link java.nio.channels.ServerSocketChannel} in non-blocking mode.
 * Simplifies input-output, handling queues of accept requests.
 */
public class AsyncServerSocketChannel1
    extends AsyncServerSocketChannel
    implements SelectorListenerUser
{
    private ServerSocketChannel channel;
    private SelectorThread selectorThread;
    private final Acceptor acceptor1;
    private SelectorListener selectorListener;

    public AsyncServerSocketChannel1() throws IOException {
        channel = ServerSocketChannel.open();
        channel.configureBlocking(false);
        selectorThread = SelectorThread.getCurrentSelectorThread();
        selectorListener=selectorThread.new SelectorListener(this);
        acceptor1=new Acceptor(selectorThread);
    }

    @Override
    public void bind(SocketAddress addr) throws IOException {
        if (addr == null) {
            throw new NullPointerException();
        }
        super.addr = addr;
        channel.socket().bind(addr);
        acceptor1.channelAccess.up();
    }

    /**
     * initiates acceptance process
     * 
     * @return AsyncSocketChannel waiting to connect
     */
    @Override
    public ListenableFuture<AsyncSocketChannel> accept() {
        if (isClosed()) {
            throw new IllegalStateException();
        }
        AsyncSocketChannel1 asc = new AsyncSocketChannel1(selectorThread);
        acceptor1.post(asc);
        return asc.connEvent;
    }

    @Override
    public synchronized void close() {
        try {
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        channel = null;
        closeEvent.post(this);
    }

    public boolean isClosed() {
        return channel == null;
    }

    @Override
    public Selector getSelector() {
        return selectorThread.selector;
    }

    @Override
    public SelectableChannel getChannel() {
        return channel;
    }

    //===================== inner classes
    
    /**
     * Serializes concurrent attempts to accept connections
     */
    class Acceptor extends Actor<AsyncSocketChannel1> {
        /** prevents simultaneous channel.accept() */
        Semafor channelAccess = new Semafor();

        public Acceptor(SelectorThread selectorThread) {
            super(selectorThread);
        }

        @Override
        protected void act(AsyncSocketChannel1 asc) throws Exception {
            SocketChannel sch;
            try {
                sch = channel.accept();
            } catch (IOException e1) {
                asc.connEvent.postFailure(e1);
                channelAccess.up();
                return;
            }
            if (sch != null) {
                asc.connEvent.postSocketChannel(sch);
                // call semafore up last, it may call act() and interstOn
                channelAccess.up();
            } else {
                input.pushback();
                selectorListener.interestOn(SelectionKey.OP_ACCEPT, acceptor1.channelAccess);
            }
        }
    }
}
