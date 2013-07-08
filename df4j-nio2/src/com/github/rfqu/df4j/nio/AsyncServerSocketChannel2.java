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
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import com.github.rfqu.df4j.core.ActorVariable;
import com.github.rfqu.df4j.core.ListenableFuture;
import com.github.rfqu.df4j.nio.AsyncServerSocketChannel;
import com.github.rfqu.df4j.nio.AsyncSocketChannel;

/**
 * Wrapper over {@link AsynchronousServerSocketChannel}.
 */
public class AsyncServerSocketChannel2 extends AsyncServerSocketChannel
//  implements CompletionHandler<AsynchronousSocketChannel, Callback<AsynchronousSocketChannel>>
{
    private volatile AsynchronousServerSocketChannel assc;
    private final Acceptor acceptor1=new Acceptor();
    
    public AsyncServerSocketChannel2() throws IOException {
        AsynchronousChannelGroup acg=AsyncChannelCroup.getCurrentACGroup();
        assc=AsynchronousServerSocketChannel.open(acg);
    }
    
    @Override
    public void bind(SocketAddress addr) throws IOException{
        if (addr==null) {
            throw new NullPointerException();
        }
        super.addr=addr;
        assc.bind(addr);
        acceptor1.channelAccess.up();
    }

    /** initiates acceptance process
     * @return AsyncSocketChannel waiting to connect
     */
    @Override
    public ListenableFuture<AsyncSocketChannel> accept() {
        AsyncSocketChannel2 res=new AsyncSocketChannel2();
        acceptor1.post(res);
        return res.connEvent;
    }

    public void close() {
        try {
            assc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        assc = null;
        closeEvent.post(this);
    }

    public boolean isClosed() {
        return closeEvent.isDone();
    }

    //====================== CompletionHandler's backend
    class Acceptor extends ActorVariable<AsyncSocketChannel2> // TODO Actor?
       implements CompletionHandler<AsynchronousSocketChannel, CompletionHandler<Void, AsynchronousSocketChannel>>
    {
        /** prevents simultaneous channel.accept() */
        protected Semafor channelAccess = new Semafor();

        @Override
        protected void act(AsyncSocketChannel2 asc) throws Exception {
            assc.accept(asc.connEvent, this);
        }

        @Override
        public void completed(AsynchronousSocketChannel result, CompletionHandler<Void, AsynchronousSocketChannel> acceptor2) {
            acceptor2.completed(null, result);
            channelAccess.up();
        }

        /** new client connection failed */
        @Override
        public void failed(Throwable exc, CompletionHandler<Void, AsynchronousSocketChannel> acceptor2) {
            acceptor2.failed(exc, null); // TODO
            if (exc instanceof AsynchronousCloseException) {
                // channel closed.
                close();
            } else {
                channelAccess.up();
            }
        }
    }
}
