package com.github.rfqu.df4j.test;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.github.rfqu.df4j.nio.AsyncServerSocketChannel;
import com.github.rfqu.df4j.nio.AsyncSocketChannel;

public interface AsyncChannelFactory {
    public AsyncServerSocketChannel newAsyncServerSocketChannel(InetSocketAddress addr, com.github.rfqu.df4j.test.AsyncServerSocketChannelTest.Server server) throws IOException;

    public AsyncSocketChannel newAsyncSocketChannel(InetSocketAddress addr) throws IOException;
}