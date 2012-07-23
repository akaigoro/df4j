/*
 * Copyright 2011-2012 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.github.rfqu.df4j.nio2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import com.github.rfqu.df4j.core.StreamPort;

public class AsyncServerSocketChannel 
  implements CompletionHandler<AsynchronousSocketChannel,Void>
{
    private AsynchronousServerSocketChannel channel;
    /** max number of accepted connections */
    private int maxConn;
    /** an accept request is pending */
    private boolean pending=false;
    private boolean opened=false;
    /** consumer of accepted connections */
    private StreamPort<AsynchronousSocketChannel> consumer;
    
    public AsyncServerSocketChannel(InetSocketAddress addr) throws IOException {
        AsynchronousChannelGroup acg=AsyncChannelCroup.getCurrentACGroup();
        channel=AsynchronousServerSocketChannel.open(acg);
        channel.bind(addr);
    }
    
    public synchronized void start(StreamPort<AsynchronousSocketChannel> consumer, int maxConn) {
        synchronized (this) {
            if (isOpened()) {
                throw new IllegalStateException("opened already");
            }
            opened=true;
        }
        this.consumer=consumer;
        this.maxConn=maxConn;
        acceptIfPossible();
    }

    private void acceptIfPossible() {
        synchronized (this) {
            if (!isOpened()) {
                throw new IllegalStateException("not opened");
            }
            if (pending) {
//                System.out.println("acceptIfPossible: not (pending)");
                return;
            }
            if (maxConn==0) {
//                System.out.println("acceptIfPossible: not (maxConn==0)");
                return;
            }
            maxConn--;
            pending=true;
        }
//        System.out.println("acceptIfPossible: yes");
        accept();
    }

    public void maxConnUp() {
        synchronized (this) {
            if (!isOpened()) {
                throw new IllegalStateException("not opened");
            }
            if (pending) {
//                System.out.println("maxConnUp: not (pending)");
                maxConn++;
                return;
            }
            pending=true;
        }
//        System.out.println("maxConnUp: yes");
        accept();
    }
    
    private void accept() {
        try {
            channel.accept(null, this);
//        } catch (ClosedChannelException e) {
        } catch (Exception e) {
            e.printStackTrace();
            close();
        }
    }

    /** new client connected */
    @Override
    public void completed(AsynchronousSocketChannel result, Void attachment) {
//        System.out.println("accepted");
        synchronized (this) {
            pending=false;
        }
        consumer.send(result);
        acceptIfPossible();
    }

    /** new client connection failed */
    @Override
    public void failed(Throwable exc, Void attachment) {
        if (exc instanceof AsynchronousCloseException) {
            try {
                close();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return;
        }
        exc.printStackTrace();
        maxConnUp(); // TODO log and set max attempt counter 
        acceptIfPossible();
    }

    public void close() {
        synchronized (this) {
            if (!isOpened()) {
                return;
            }
            opened=false;
        }
        try {
            channel.close();
        } catch (IOException e) {
        }
        consumer.close();
    }
    
    public AsynchronousServerSocketChannel getChannel() {
        return channel;
    }

    public boolean isOpened() {
        return opened;
    }
}
