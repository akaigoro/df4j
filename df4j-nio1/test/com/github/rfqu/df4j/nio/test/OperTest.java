package com.github.rfqu.df4j.nio.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.rfqu.df4j.core.ListenableFuture;
import com.github.rfqu.df4j.nio.AsyncServerSocketChannel;
import com.github.rfqu.df4j.nio.AsyncServerSocketChannel1;
import com.github.rfqu.df4j.nio.AsyncSocketChannel;
import com.github.rfqu.df4j.nio.AsyncSocketChannel1;
import com.github.rfqu.df4j.nio.SocketIORequest;

public class OperTest {
    static final int BUF_SIZE = 2048;
    // static final InetSocketAddress local9990 = new InetSocketAddress("localhost", 9990);
    static final InetSocketAddress local9990 = new InetSocketAddress("localhost", 8007);

    AsyncServerSocketChannel assc;

    @Before
    public void init() throws IOException {
        assc = new AsyncServerSocketChannel1();
        assc.bind(local9990);
    }

    @After
    public void close() {
        assc.close();
    }

    AsyncSocketChannel1 serverConn;
    AsyncSocketChannel1 clientConn;

    /**
     * open connections on both sides, server first
     */
    public void smokeTest1() throws Exception {
        ListenableFuture<AsyncSocketChannel> connectionEvent = assc.accept();
        clientConn = new AsyncSocketChannel1();
        clientConn.connect(local9990);

        Thread.sleep(50);
        // assertTrue(connectionEvent.isDone());
        // assertTrue(clientConn.getConnEvent().isDone());
        serverConn = (AsyncSocketChannel1) connectionEvent.get();
    }

    @Test
    public void readWriteTest() throws Exception {
        smokeTest1(); // open connections

        for (int k = 0; k < 24; k++) {
            WriteRequest swrequest = new WriteRequest();
            serverConn.write(swrequest);
        }

        ReadRequest srrequest = new ReadRequest();
        serverConn.read(srrequest);

        Thread.sleep(3000);
        for (int k = 0; k < 2; k++) {
            ReadRequest crrequest = new ReadRequest();
            clientConn.read(crrequest);
        }
        Thread.sleep(3000);
        for (int k = 0; k < 24; k++) {
            WriteRequest swrequest = new WriteRequest();
            serverConn.write(swrequest);
        }

        Thread.sleep(3000);
        clientConn.write(new WriteRequest());

        Thread.sleep(3000);
        for (int k = 0; k < 12; k++) {
            ReadRequest crrequest = new ReadRequest();
            clientConn.read(crrequest);
        }
    }

    static class ReadRequest extends SocketIORequest<ReadRequest> {
        ByteBuffer buff;

        public ReadRequest() {
            super(ByteBuffer.allocate(BUF_SIZE));
            buff = super.getBuffer();
        }

    }

    static class WriteRequest extends SocketIORequest<WriteRequest> {
        ByteBuffer buff;

        public WriteRequest() {
            super(ByteBuffer.allocate(BUF_SIZE));
            buff = super.getBuffer();
            for (int k = 0; k < BUF_SIZE / 4; k++) {
                buff.putInt(k);
            }
        }

    }
}