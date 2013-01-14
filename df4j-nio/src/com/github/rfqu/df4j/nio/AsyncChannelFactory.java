package com.github.rfqu.df4j.nio;

import java.io.IOException;
import java.net.SocketAddress;

import com.github.rfqu.df4j.core.Callback;

public interface AsyncChannelFactory {
    public AsyncServerSocketChannel newAsyncServerSocketChannel(SocketAddress addr, Callback<AsyncSocketChannel> callback) throws IOException;

    public AsyncSocketChannel newAsyncSocketChannel(SocketAddress addr) throws IOException;
}