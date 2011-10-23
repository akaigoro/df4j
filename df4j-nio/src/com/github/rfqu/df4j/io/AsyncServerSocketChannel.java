/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import com.github.rfqu.df4j.core.MessageQueue;

public class AsyncServerSocketChannel extends AsyncChannel {
    ServerSocketChannel serverChannel;
    MessageQueue<AsyncSocketChannel> acceptors=new MessageQueue<AsyncSocketChannel>();

    public AsyncServerSocketChannel(AsyncSelector selector) {
        super(selector);
    }

    public AsyncServerSocketChannel(AsyncSelector selector, InetSocketAddress listenAddr) throws IOException {
        super(selector);
        connect(listenAddr);
    }

    public void connect(InetSocketAddress listenAddr) throws IOException {
        serverChannel = ServerSocketChannel.open();
        serverChannel.socket().bind(listenAddr);
        serverChannel.configureBlocking(false);
        interestOn(SelectionKey.OP_ACCEPT);
    }

    /** 
     * @param acceptor AsyncSocketChannel wishing to connect
     * @throws IOException
     * @throws ClosedChannelException
     */
    public synchronized void accept(AsyncSocketChannel acceptor) throws IOException {
        SocketChannel soketCh = serverChannel.accept();
        if (soketCh!=null) {
            acceptor.connCompleted(soketCh);
        } else {
            acceptors.enqueue(acceptor);
        }
    }

    @Override
    void notify(SelectionKey key) {
        try {
            for (;;) {
                synchronized (acceptors) {
                    if (acceptors.isEmpty()) {
                        return;
                    }
                }
                if (serverChannel==null || !serverChannel.isOpen()) {
                    return;
                }
                SocketChannel soketCh = serverChannel.accept();
                if (soketCh==null) {
                    return;
                }
                AsyncSocketChannel acceptor;
                synchronized (acceptors) {
                    acceptor = acceptors.poll();
                }
                acceptor.connCompleted(soketCh);            
            }
        } catch (ClosedChannelException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public ServerSocketChannel getChannel() {
        return serverChannel;
    }

    public void close() throws IOException {
//        interestOff(SelectionKey.OP_ACCEPT);
        serverChannel.close();
        serverChannel=null;
    }
    
}
