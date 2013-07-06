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
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import com.github.rfqu.df4j.core.ActorVariable;
import com.github.rfqu.df4j.core.ListenableFuture;

/**
 * Wrapper over {@link java.nio.channels.ServerSocketChannel} in non-blocking mode.
 * Simplifies input-output, handling queues of accept requests.
 */
public class AsyncServerSocketChannel1 extends AsyncServerSocketChannel {
    private ServerSocketChannel channel;
    private SelectorThread selectorThread;
    private final Acceptor acceptor1=new Acceptor();
    private SelectorListener selectorListener;

    public AsyncServerSocketChannel1() throws IOException {
        selectorThread = SelectorThread.getCurrentSelectorThread();
        channel = ServerSocketChannel.open();
        channel.configureBlocking(false);
        selectorListener=new SelectorListener(selectorThread.selector);
    }

    @Override
    public void bind(SocketAddress addr) throws IOException {
        if (addr == null) {
            throw new NullPointerException();
        }
        super.addr = addr;
        channel.socket().bind(addr);
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

    //===================== inner classes
    
    /**
     * Serializes concurrent attempts to accept connections
     */
    class Acceptor extends ActorVariable<AsyncSocketChannel1> // TODO Actor?
    {
        /** prevents simultaneous channel.accept() */
        protected Semafor channelAccess = new Semafor();

        @Override
        protected void act(AsyncSocketChannel1 asc) throws Exception {
//            assc.accept(asc.connEvent, this);
            try {
                SocketChannel sch = channel.accept();
                if (sch != null) {
                    asc.connEvent.postSocketChannel(sch);
                } else {
                    // switch to selector-helped mode
                    selectorListener.listen(asc);
                }
            } catch (IOException e) {
                asc.connEvent.postFailure(e);
            }

        }

        public void completed(SocketChannel sch, AsyncSocketChannel1 asc) {
            asc.connEvent.postSocketChannel(sch);
            channelAccess.up();
        }

        /** new client connection failed */
        public void failed(Throwable exc, AsyncSocketChannel1 asc) {
            asc.connEvent.postFailure(exc); // TODO
            if (exc instanceof AsynchronousCloseException) {
                // channel closed.
                close();
            } else {
                channelAccess.up();
            }
        }
    }

    class SelectorListener extends SelectorEventListener {
        AsyncSocketChannel1 currentAsc;
        
        public SelectorListener(Selector selector) {
            super(selector);
        }

        public void listen(AsyncSocketChannel1 asc) {
            if (currentAsc!=null) {
                throw new IllegalStateException("pending accept");
            }
            currentAsc=asc;
            super.listen(SelectionKey.OP_ACCEPT);
        }

        /** called on selector thread
         * 
         */
        @Override
        public void run(SelectionKey key) {
            // first repeat attempt to connect, as some time passed
            // since previous attempt, and there was no registration
            SocketChannel sch;
            try {
                sch = channel.accept();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
                return;
            }
            if (sch != null) {
                AsyncSocketChannel1 asc=currentAsc;
                currentAsc=null;
                acceptor1.completed(sch, asc);
            } else {
                // now register to listen accept requests
                interestOn(SelectionKey.OP_ACCEPT);
            }
        }

        protected void stop() {
            SelectionKey key = channel.keyFor(selectorThread.selector);
            if (key==null || !key.isValid()) {
                return;
            }
            key.cancel();
            channel=null;
        }
        
       @Override
        protected void channelClosed() {
            // TODO Auto-generated method stub
            
        }
    }

}
