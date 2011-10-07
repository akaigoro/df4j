package com.github.rfqu.df4j.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;

public class AsyncServerSocketChannel extends AsyncChannel {
    AsynchronousServerSocketChannel serverChannel;
    
    public AsyncServerSocketChannel(InetSocketAddress addr) throws IOException {
        serverChannel=AsynchronousServerSocketChannel.open(getGroup());
        serverChannel.bind(addr);
    }

    public AsynchronousServerSocketChannel getChannel() {
        return serverChannel;
    }
    
    public void close() throws IOException {
        serverChannel.close();
    }
}
