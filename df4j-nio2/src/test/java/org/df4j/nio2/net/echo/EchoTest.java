package org.df4j.nio2.net.echo;

import org.df4j.core.node.messagestream.PickPoint;
import org.df4j.nio2.net.AsyncServerSocketChannel;
import org.df4j.nio2.net.BaseClientConnection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;

public  class EchoTest {
    static final int BUF_SIZE = 128;
    static final SocketAddress local9990 = new InetSocketAddress("localhost", 9990);
    static final Charset charset = Charset.defaultCharset();

    AsyncServerSocketChannel assc;
    ConnectionSource csource = new ConnectionSource(1000);

    @Before
    public void init() throws IOException {
        assc = new AsyncServerSocketChannel(local9990, 4);
        csource.subscribe(assc);
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
        BaseClientConnection clientConn = new BaseClientConnection(local9990);
        ByteBuffer buf=ByteBuffer.allocate(128);
        byte[] src = message.getBytes(charset);
        buf.put(src);
        clientConn.writer.post(buf);
        PickPoint<ByteBuffer> pickPoint = new PickPoint<>();
        clientConn.reader.subscribe(pickPoint);
        ByteBuffer buf2 = pickPoint.asFuture().get(2, TimeUnit.SECONDS);
        String reply=new String(buf2.array(), 0, src.length, charset);
        assertEquals(message, reply);
    }
}