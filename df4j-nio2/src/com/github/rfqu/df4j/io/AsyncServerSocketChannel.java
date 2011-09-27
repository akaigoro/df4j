package com.github.rfqu.df4j.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.concurrent.ExecutorService;

import com.github.rfqu.df4j.core.Actor;

public class AsyncServerSocketChannel extends AsyncChannel {
    AsynchronousServerSocketChannel channel;
    
    protected AsyncServerSocketChannel(AsynchronousServerSocketChannel ch) {
        this.channel=ch;
    }

    public AsynchronousServerSocketChannel getChannel() {
        return channel;
    }
    
    public static AsyncServerSocketChannel open() throws IOException {
        ExecutorService executor=Actor.getCurrentExecutor();
        AsynchronousChannelGroup acg=getGroup(executor);
        AsynchronousServerSocketChannel ch = AsynchronousServerSocketChannel.open(acg);
        return new AsyncServerSocketChannel(ch);
    }

    public void bind(InetSocketAddress addr) throws IOException {
        channel.bind(addr);
    }

    public void close() throws IOException {
        channel.close();
    }
}
