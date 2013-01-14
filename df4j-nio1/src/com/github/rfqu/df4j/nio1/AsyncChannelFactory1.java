package com.github.rfqu.df4j.nio1;

import java.io.IOException;
import java.net.SocketAddress;

import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.nio.AsyncChannelFactory;
import com.github.rfqu.df4j.nio.AsyncServerSocketChannel;
import com.github.rfqu.df4j.nio.AsyncSocketChannel;

public class AsyncChannelFactory1 implements AsyncChannelFactory {
    @Override
    public AsyncServerSocketChannel newAsyncServerSocketChannel
       (SocketAddress addr, Callback<AsyncSocketChannel> callback)
            throws IOException
    {
        return new AsyncServerSocketChannel1(addr, callback);
    }

    @Override
    public AsyncSocketChannel newAsyncSocketChannel(SocketAddress addr) throws IOException {
        return new AsyncSocketChannel1(addr);
    }

}