package com.github.rfqu.df4j.nio.tutorial;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Random;

import com.github.rfqu.df4j.core.Port;
import com.github.rfqu.df4j.nio.AsyncSocketChannel;
import com.github.rfqu.df4j.nio.LimitedServer;

/** for each connected client, sends data using {@link DataFeeder}..
 */
class DataServer extends LimitedServer implements Port<AsyncSocketChannel>{

    public DataServer(SocketAddress addr) throws IOException {
        super(addr);
    }

    protected void start(int maxConnCount) {
        super.start(this, 1, maxConnCount);
    }

    /** a client connected */
    @Override
    public void post(AsyncSocketChannel asc) {
        int size=5000+new Random().nextInt(10000);
        new DataFeeder(asc, size).write();
    }
}