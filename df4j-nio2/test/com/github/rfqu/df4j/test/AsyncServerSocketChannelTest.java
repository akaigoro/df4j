package com.github.rfqu.df4j.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.core.CallbackFuture;
import com.github.rfqu.df4j.nio2.AsyncServerSocketChannel;
import com.github.rfqu.df4j.nio2.AsyncSocketChannel;

public class AsyncServerSocketChannelTest {
    static final int BUF_SIZE = 128;
    static final InetSocketAddress local9990 = new InetSocketAddress("localhost", 9990);
    static final AtomicInteger ids=new AtomicInteger(); // for DEBUG

    static final int maxConn=22;
    Server server;
    
    @Before
    public void before() throws IOException {
        server=new Server(local9990, maxConn);
    }

    @After
    public void after() {
        try {
            server.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * tests that overall connection count can be more than maxConn
     */
    @Test
    public void maxConnTest() throws Exception {
        ArrayList<ClientConnection>allConns=new ArrayList<ClientConnection>();
        int half=maxConn/2;
        int clConns=maxConn+half;
        for (int k=0; k<clConns; k++) {
            ClientConnection conn = new ClientConnection(local9990);
            allConns.add(conn);
            conn.get();
        }
        Thread.sleep(100); // wait server to accept connections
        // only maxConn opened
        assertEquals(maxConn, server.channelCounter);

        // the rest opened
        for (int k=0; k<half; k++) {
            server.assch.upConnNumber();
        }
        Thread.sleep(100); // wait server to accept connections
        assertEquals(clConns, server.channelCounter);
        assertTrue(server.allOpened);
    }


    static class Server implements Callback<AsyncSocketChannel> {        
        AsyncServerSocketChannel assch;    
        ArrayList<AsyncSocketChannel>allConns=new ArrayList<AsyncSocketChannel>();
        int channelCounter=0;
        boolean allOpened=true;
            
        public Server(InetSocketAddress addr, int maxConn) throws IOException {
            assch=new AsyncServerSocketChannel(addr, this, maxConn);
        }
        
        @Override
        public void send(AsyncSocketChannel channel) {
            channelCounter++;
            if (channel.isClosed()) {
                allOpened=false;
            }
            allConns.add(channel);
        }

        /** closing requested */
        protected void close() throws Exception {
            assch.close();
        }

        @Override
        public void sendFailure(Throwable exc) {
            // TODO Auto-generated method stub
            
        }
    }

    static class ClientConnection {
        AsyncSocketChannel conn;
        CallbackFuture<AsynchronousSocketChannel> listener=new CallbackFuture<AsynchronousSocketChannel>();
        
        ClientConnection(InetSocketAddress addr) throws IOException {
            conn=new AsyncSocketChannel(addr);
            conn.addListener(listener);
        }

        public void get() throws InterruptedException, ExecutionException {
        	listener.get();
        }
    }
    
}