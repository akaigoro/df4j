package com.github.rfqu.df4j.nio;

import java.io.IOException;

import com.github.rfqu.df4j.nio.AsyncChannelFactory;
import com.github.rfqu.df4j.nio.AsyncServerSocketChannel;
import com.github.rfqu.df4j.nio.AsyncSocketChannel;

public class AsyncChannelFactory2 extends AsyncChannelFactory {

    @Override
    public AsyncServerSocketChannel newAsyncServerSocketChannel()
        throws IOException
    {
        return new AsyncServerSocketChannel2();
    }

    @Override
    public AsyncSocketChannel newAsyncSocketChannel()
        throws IOException
    {
        return new AsyncSocketChannel2();
    }

}