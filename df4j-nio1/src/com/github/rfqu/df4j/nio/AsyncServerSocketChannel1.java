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
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import com.github.rfqu.df4j.core.ActorVariable;
import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.nio.AsyncServerSocketChannel;
import com.github.rfqu.df4j.nio.AsyncSocketChannel;

/**
 * Wrapper over {@link java.nio.channels.ServerSocketChannel} in non-blocking mode.
 * Simplifies input-output, handling queues of accept requests.
 */
public class AsyncServerSocketChannel1 extends AsyncServerSocketChannel
	implements Runnable, SelectorEventListener
 {
    private ServerSocketChannel channel;
    private SelectorThread selectorThread;

    public AsyncServerSocketChannel1() throws IOException {
        super(addr);
        selectorThread = SelectorThread.getCurrentSelectorThread();
        channel = ServerSocketChannel.open();
        channel.configureBlocking(false);
        channel.socket().bind(addr);
    }

    /**
     * initiates acceptance process when the channel is free
     */
    @Override
    public AsyncSocketChannel accept() {
        if (isClosed()) {
            throw new IllegalStateException();
        }
        AsyncSocketChannel1 asc=new AsyncSocketChannel1();
        boolean allAccepted=tryAccept();
        if (allAccepted) {
            return; // 
        }
        // switch to selector-helped mode
        selectorThread.execute(this);
    }

    public void up() {
        up(1);
    }

    /**
     * Tries to accept max number of client connections
     * @return true if maxConn limit is exhausted
     */
    boolean tryAccept() {
        int maxConn = 0;
        for (; maxConn>0; maxConn--) {
            Callback<AsyncSocketChannel> acceptor = null;
            try {
                if (channel==null) {
                    break; // connection closed
                }
                SocketChannel sch = channel.accept();
                if (sch==null) {
                    break;
                }
                acceptor.post();
            } catch (IOException e) {
                acceptor.postFailure(e);
            }
        }
        return maxConn==0;
    }

    @Override
    public synchronized void run() {
        // first repeat attempt to connect, as some time passed
        // since previous attempt, and there was no registration
        boolean allAccepted=tryAccept();
        if (allAccepted) {
            return; // 
        }
        // now register to listen accept requests
        try {
            selectorThread.registerNow(channel, SelectionKey.OP_ACCEPT, this);
        } catch (ClosedChannelException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            close();
        }
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
        return channel==null;
    }

    // ========================= Actor's backend

    @Override
    protected void act(Callback<AsyncSocketChannel> acceptor) throws Exception {
        // TODO Auto-generated method stub
        
    }

    // ========================= ServerSocketEventListener backend

    /** new client wants to connect */
    @Override
    public synchronized void onSelectorEvent(SelectionKey key) {
        boolean allAccepted=tryAccept();
        if (allAccepted) {
            // return back to initial state, with no registration
            key.cancel();
        }
    }
}
