package com.github.rfqu.df4j.io;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.concurrent.ExecutorService;

import com.github.rfqu.df4j.core.Actor;

public class AsyncServerSocketChannel extends AsyncChannel {

    public static AsynchronousServerSocketChannel open() throws IOException {
        ExecutorService executor=Actor.getCurrentExecutor();
        AsynchronousChannelGroup acg=getGroup(executor);
        return AsynchronousServerSocketChannel.open(acg);
    }
}
