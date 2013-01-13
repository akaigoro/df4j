package com.github.rfqu.df4j.test;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.github.rfqu.df4j.nio.AsyncServerSocketChannel;
import com.github.rfqu.df4j.nio2.AsyncServerSocketChannel2;
import com.github.rfqu.df4j.nio2.AsyncSocketChannel2;

public class AsyncServerSocketChannelTest2 extends AsyncServerSocketChannelTest {
    @Override
    protected AsyncServerSocketChannel newAsyncServerSocketChannel(InetSocketAddress addr, com.github.rfqu.df4j.test.AsyncServerSocketChannelTest.Server server) throws IOException {
        return new AsyncServerSocketChannel2(addr, server);
    }

    protected Connection newConnection() throws IOException {
        return new Connection(new AsyncSocketChannel2(local9990));
    }

}