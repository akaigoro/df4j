/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.nio2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;

import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.core.DataflowVariable;

/**
 * Wrapper over {@link java.nio.channels.AsynchronousServerSocketChannel}.
 * Simplifies input-output, handling queues of accept requests.
 * @author rfqu
 *
 */
public class AsyncServerSocketChannel extends DataflowVariable
  implements CompletionHandler<AsynchronousSocketChannel,Void>
{
    private Switch pending=new Switch();
    private Semaphore maxConnLimit=new Semaphore();
    private AsynchronousServerSocketChannel channel;
    private Callback<AsynchronousSocketChannel> consumer;
    
    public AsyncServerSocketChannel(InetSocketAddress addr, Callback<AsynchronousSocketChannel> consumer, int maxConn)
                throws IOException
    {
        if (maxConn<=0) {
            throw new IllegalArgumentException("maxConn="+maxConn+"; should be positive");
        }
        if (consumer==null) {
            throw new NullPointerException();
        }
        this.consumer=consumer;
        AsynchronousChannelGroup acg=AsyncChannelCroup.getCurrentACGroup();
        channel=AsynchronousServerSocketChannel.open(acg);
        channel.bind(addr);
        maxConnLimit.up(maxConn);
        pending.on(); // allow accept
    }
    
    public void upConnNumber() {
        maxConnLimit.up();
    }

    public void close() throws IOException {
        AsynchronousServerSocketChannel channelLoc;
        synchronized (this) {
            if (channel==null) {
                return;
            }
            channelLoc=channel;
            channel = null;
        }
        channelLoc.close();
        consumer.sendFailure(new ClosedChannelException());
    }
    
    public AsynchronousServerSocketChannel getChannel() {
        return channel;
    }

    public boolean isOpened() {
        return channel!=null;
    }

    //======================= backend

    @Override
    protected void act() {
        channel.accept(null, this);
    }
    
    /** new client connected */
    @Override
    public void completed(AsynchronousSocketChannel result, Void attachment) {
        consumer.send(result);
    }

    /** new client connection failed */
    @Override
    public void failed(Throwable exc, Void attachment) {
        Callback<AsynchronousSocketChannel> consumerLoc;
        synchronized (this) {
            if (consumer==null) {
                return;
            }
            consumerLoc = consumer;
        }
        consumerLoc.sendFailure(exc);
    }

}
