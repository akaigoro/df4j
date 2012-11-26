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
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import com.github.rfqu.df4j.core.Callback;

/**
 * Wrapper over {@link java.nio.channels.ServerSocketChannel} in non-blocking mode.
 * Simplifies input-output, handling queues of accept requests.
 * 
 * @author rfqu
 */
public class AsyncServerSocketChannel {
    private int maxConn;
    protected volatile boolean interestOn=false;
    private ServerSocketChannel ssc;
    private SelectorThread currentSelectorThread ;
    private Callback<AsyncSocketChannel> consumer;
    private MyListener selectorListener=new MyListener();
    protected volatile boolean closed=false;
    
    public AsyncServerSocketChannel(InetSocketAddress addr, Callback<AsyncSocketChannel> consumer, int maxConn)
                throws IOException
    {
        if (maxConn<=0) {
            throw new IllegalArgumentException("maxConn="+maxConn+"; should be positive");
        }
        if (consumer==null) {
            throw new NullPointerException();
        }
        this.maxConn=maxConn;
        ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        ssc.socket().bind(addr);
        currentSelectorThread=SelectorThread.getCurrentSelectorThread();
        currentSelectorThread.register(ssc, SelectionKey.OP_ACCEPT, selectorListener);
        interestOn=true;
        this.consumer=consumer;
    }
    
    public synchronized void upConnNumber() {
        maxConn++;
        if (!interestOn) { 
            // interest was switched off
            currentSelectorThread.setInterest(ssc, SelectionKey.OP_ACCEPT);
        }
    }

    public void close() {
        closed=true;
        if (ssc!=null) {
            currentSelectorThread.deregister(ssc);
            try {
                ssc.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                ssc=null;
            }
        }
        currentSelectorThread=null;
    }
    
    public boolean isOpened() {
        return ssc!=null;
    }

    //========================= backend inner classes
    
    /** wrapped in a separate class in order not to expose
     * callback methods
     */
    class MyListener implements SocketEventListener{
        
        /** new client wants to connect */
        @Override
        public void accept(SelectionKey key) {
            synchronized (AsyncServerSocketChannel.this) {
                if (maxConn>0) {
                    maxConn--;
                } else if (interestOn) {
                    currentSelectorThread.setInterest(ssc, 0);
                    interestOn=false;
                }
            }
            try {
                SocketChannel sch = ssc.accept();
                AsyncSocketChannel res=new AsyncSocketChannel(currentSelectorThread, sch);
                consumer.send(res);
            } catch (IOException exc) {
                consumer.sendFailure(exc);
            }
        }

        @Override
        public void connect(SelectionKey key) {}

        @Override
        public void read(SelectionKey key) {}

        @Override
        public void write(SelectionKey key) {}

        @Override
        public void close() {
            AsyncServerSocketChannel.this.close();
        }
    }
}
