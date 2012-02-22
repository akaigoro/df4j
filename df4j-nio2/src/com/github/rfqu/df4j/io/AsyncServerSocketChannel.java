package com.github.rfqu.df4j.io;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;

import com.github.rfqu.df4j.core.Actor;

public class AsyncServerSocketChannel extends Actor<AsyncSocketChannel> {
    AsynchronousServerSocketChannel channel;
    
    public AsyncServerSocketChannel() throws IOException {
        AsynchronousChannelGroup acg=AsyncChannelCroup.getCurrentACGroup();
        channel=AsynchronousServerSocketChannel.open(acg);
    }

    public AsynchronousServerSocketChannel getChannel() {
        return channel;
    }
    /*
    public static AsynchronousServerSocketChannel open() throws IOException {
        AsynchronousChannelGroup acg=AsyncChannelCroup.getCurrentACGroup();
        return AsynchronousServerSocketChannel.open(acg);
    }
*/
    @Override
    protected void act(AsyncSocketChannel connection) throws Exception {
        super.stop();
        channel.accept(this, connection);
    }

    @Override
    protected void complete() throws Exception {
        channel.close();
    }
    
    public void acceptCompleted() {
        super.start();
    }

}
