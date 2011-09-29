package com.github.rfqu.df4j.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.concurrent.ExecutorService;

import com.github.rfqu.df4j.core.Actor;

public class AsyncServerSocketChannel extends AsyncChannel {
    AsynchronousServerSocketChannel serverChannel;
    
    public AsyncServerSocketChannel(InetSocketAddress addr) throws IOException {
        ExecutorService executor=Actor.getCurrentExecutor();
        AsynchronousChannelGroup acg=getGroup(executor);
        AsynchronousServerSocketChannel ch = AsynchronousServerSocketChannel.open(acg);
        ch.bind(addr);
        this.serverChannel=ch;
    }

    public AsynchronousServerSocketChannel getChannel() {
        return serverChannel;
    }
    
    public void close() throws IOException {
        serverChannel.close();
    }
}
