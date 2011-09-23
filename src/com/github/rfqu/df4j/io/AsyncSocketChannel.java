package com.github.rfqu.df4j.io;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutorService;

import com.github.rfqu.df4j.core.Actor;

public class AsyncSocketChannel extends AsyncChannel {

    public static AsynchronousSocketChannel open() throws IOException {
        ExecutorService executor=Actor.getCurrentExecutor();
        AsynchronousChannelGroup acg=getGroup(executor);
        return AsynchronousSocketChannel.open(acg);
    }
}
