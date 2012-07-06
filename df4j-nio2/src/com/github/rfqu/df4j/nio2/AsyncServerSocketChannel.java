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
import java.nio.channels.CompletionHandler;

import com.github.rfqu.df4j.core.Actor;

public class AsyncServerSocketChannel 
  extends Actor<AsyncSocketChannel> 
  implements CompletionHandler<AsynchronousSocketChannel,AsyncSocketChannel>
{
    private AsynchronousServerSocketChannel channel;
    private Switch channelAcc=new Switch(); // channel accessible   
    
    public AsyncServerSocketChannel(InetSocketAddress addr) throws IOException {
        AsynchronousChannelGroup acg=AsyncChannelCroup.getCurrentACGroup();
        channel=AsynchronousServerSocketChannel.open(acg);
        channel.bind(addr);
        channelAcc.on();
    }

    public AsynchronousServerSocketChannel getChannel() {
        return channel;
    }

    @Override
    protected void act(AsyncSocketChannel connection) throws Exception {
        channelAcc.off();
        channel.accept(connection, this);
    }

    @Override
    protected void complete() throws Exception {
        channel.close();
    }
    
    @Override
    public void completed(AsynchronousSocketChannel result, AsyncSocketChannel attachment) {
        channelAcc.on();
        attachment.completed(null, result);
    }

    @Override
    public void failed(Throwable exc, AsyncSocketChannel attachment) {
        channelAcc.on();
        attachment.failed(exc, null);
    }
}
