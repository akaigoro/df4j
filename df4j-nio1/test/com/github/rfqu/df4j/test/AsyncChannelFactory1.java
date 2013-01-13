package com.github.rfqu.df4j.test;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.github.rfqu.df4j.nio.AsyncServerSocketChannel;
import com.github.rfqu.df4j.nio.AsyncSocketChannel;
import com.github.rfqu.df4j.nio1.AsyncServerSocketChannel1;
import com.github.rfqu.df4j.nio1.AsyncSocketChannel1;

public class AsyncChannelFactory1 implements AsyncChannelFactory {
    @Override
    public AsyncServerSocketChannel newAsyncServerSocketChannel(InetSocketAddress addr, com.github.rfqu.df4j.test.AsyncServerSocketChannelTest.Server server) throws IOException {
        return new AsyncServerSocketChannel1(addr, server);
    }

    @Override
    public AsyncSocketChannel newAsyncSocketChannel(InetSocketAddress addr) throws IOException {
        return new AsyncSocketChannel1(addr);
    }

}