package org.df4j.nio2.net.echo;

import org.df4j.core.node.messagestream.PickPoint;
import org.df4j.nio2.net.AsyncServerSocketChannel;
import org.df4j.nio2.net.ClientConnection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;

public  class EchoTest {
    static final int BUF_SIZE = 128;
    static final SocketAddress local9990 = new InetSocketAddress("localhost", 9990);
    static final Charset charset = Charset.defaultCharset();

    AsyncServerSocketChannel assc;
    ConnectionSource csource;

    @Before
    public void init() throws IOException {
        assc = new AsyncServerSocketChannel(local9990, 2);
        csource = new ConnectionSource(2);
        csource.subscribe(assc);
        assc.start();
        csource.start();
    }

    @After
    public void close() {
        assc.close();
    }
    
    /**
     * send a message from client to server 
     */
    @Test
    public void smokeIOTest() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        String message="hi there";
        ClientConnection clientConn = new ClientConnection(local9990);
        ByteBuffer buf=ByteBuffer.allocate(128);
        byte[] src = message.getBytes(charset);
        buf.put(src);
        clientConn.writer.post(buf);
        ByteBuffer buf2=ByteBuffer.allocate(128);
        clientConn.reader.post(buf);

        PickPoint<ByteBuffer> pickPoint = new PickPoint<>();
        clientConn.reader.subscribe(pickPoint);
        Future<ByteBuffer> future = pickPoint.asFuture();
        ByteBuffer buf3 = future.get(2000, TimeUnit.SECONDS);
        String reply=new String(buf3.array(), 0, src.length, charset);
        assertEquals(message, reply);
    }
}