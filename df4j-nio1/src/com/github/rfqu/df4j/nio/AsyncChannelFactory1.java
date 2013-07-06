package com.github.rfqu.df4j.nio;

import java.io.IOException;

public class AsyncChannelFactory1 extends AsyncChannelFactory {
    @Override
    public AsyncServerSocketChannel newAsyncServerSocketChannel() throws IOException
    {
        return new AsyncServerSocketChannel1();
    }

    @Override
    public AsyncSocketChannel newAsyncSocketChannel() throws IOException {
        return new AsyncSocketChannel1();
    }

}