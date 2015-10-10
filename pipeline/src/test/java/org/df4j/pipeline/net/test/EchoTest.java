package org.df4j.pipeline.net.test;

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.df4j.pipeline.core.Pipeline;
import org.df4j.pipeline.core.SinkNode;
import org.df4j.pipeline.df4j.core.DFContext;
import org.df4j.pipeline.io.net.AsyncServerSocketChannel;
import org.df4j.pipeline.io.net.AsyncSocketChannel;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public  class EchoTest {
    static final int BUF_SIZE = 128;
    static final SocketAddress local9990 = new InetSocketAddress("localhost", 9990);

    @BeforeClass
    public static void initClass() {
        DFContext.setSingleThreadExecutor();
    }

    AsyncServerSocketChannel assc; 
    ClientConnection clientConn;
        
    @Before
    public void init() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        assc = new AsyncServerSocketChannel(local9990, 20);
        Pipeline acceptor=new Pipeline();
        acceptor.setSource(assc).setSink(new Reactor()).start();
        
        AsyncSocketChannel client=new AsyncSocketChannel(local9990);
        clientConn=new ClientConnection(client);
        clientConn.start();
    }

    @After
    public void close() {
        clientConn.close();
        assc.close();
    }
    
    /**
     * send a message from client to server 
     */
    @Test
    public void smokeIOTest() throws Exception {
        try {
            String message="hi there";
            clientConn.write(message);
            clientConn.write("\n");
            String reply=clientConn.read();
            assertEquals(message, reply);
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    /**
     * accepted connections, formatted as {@link AsyncSocketChannel}, arrive to {@link myInput}.
     * They should be returned to the peer {@link AsyncServerSocketChannel}.
     * For each connection, an instance of {$link Connection} is created.
     */
    class Reactor extends SinkNode<AsyncSocketChannel> {
        
        @Override
        protected void act(AsyncSocketChannel channel) {
            channel.reader.injectBuffers(2, BUF_SIZE);
            Pipeline echoPipe=new Pipeline()
            .setSource(channel.reader)
            .setSink(channel.writer)
            .start();
        }
        
    }
    
    public static void main(String[] args) {
        EchoTest ct = new EchoTest();
        EchoTest.initClass();
        try {
            ct.init();
            ct.smokeIOTest();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}