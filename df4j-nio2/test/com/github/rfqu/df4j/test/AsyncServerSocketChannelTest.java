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

import com.github.rfqu.df4j.core.Actor;
import com.github.rfqu.df4j.core.CallbackFuture;
import com.github.rfqu.df4j.core.StreamPort;
import com.github.rfqu.df4j.nio2.AsyncServerSocketChannel;
import com.github.rfqu.df4j.nio2.AsyncSocketChannel;

public class AsyncServerSocketChannelTest {
    static final int BUF_SIZE = 128;
    static final InetSocketAddress local9999 = new InetSocketAddress("localhost", 9998);
    static final AtomicInteger ids=new AtomicInteger(); // for DEBUG

    static final int maxConn=22;
    Server server;
    
    @Before
    public void before() throws IOException {
        server=new Server(local9999, maxConn);
    }

    @After
    public void after() throws IOException {
        server.close();
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
            ClientConnection conn = new ClientConnection(local9999);
            allConns.add(conn);
            conn.get();
        }
        Thread.sleep(100); // wait server to accept connections
        // only maxConn opened
        assertEquals(maxConn, server.channelCounter);

        // the rest opened
        for (int k=0; k<half; k++) {
            server.assch.maxConnUp();
        }
        Thread.sleep(100); // wait server to accept connections
        assertEquals(clConns, server.channelCounter);
        assertTrue(server.allOpened);
    }


    static class Server extends Actor<AsyncSocketChannel> {        
        AsyncServerSocketChannel assch;    
        ArrayList<AsyncSocketChannel>allConns=new ArrayList<AsyncSocketChannel>();
        CallbackFuture<Boolean> unbounded=new CallbackFuture<Boolean>();
        int channelCounter=0;
        boolean allOpened=true;
            
        public Server(InetSocketAddress addr, int maxConn) throws IOException {
            assch=new AsyncServerSocketChannel(addr);
            assch.open(inport, maxConn);
        }
        
		StreamPort<AsynchronousSocketChannel> inport=new StreamPort<AsynchronousSocketChannel>() {
			@Override
			public void send(AsynchronousSocketChannel m) {
				Server.this.send(new AsyncSocketChannel(m));
			}

            @Override
            public void close() {
                // TODO Auto-generated method stub
                
            }			
		};
        

        @Override
        protected void act(AsyncSocketChannel channel) throws Exception {
            channelCounter++;
            if (channel.isClosed()) {
                allOpened=false;
            }
            allConns.add(channel);
        }

        /** closing requested */
        @Override
        protected void complete() throws Exception {
            assch.close();
            unbounded.send(Boolean.TRUE);
        }
    }

    static class ClientConnection {
        AsyncSocketChannel conn;
        CallbackFuture<AsynchronousSocketChannel> listener=new CallbackFuture<AsynchronousSocketChannel>();
        
        ClientConnection(InetSocketAddress addr) throws IOException {
            conn=new AsyncSocketChannel(addr);
            conn.addConnectListener(listener);
        }

        public void get() throws InterruptedException, ExecutionException {
        	listener.get();
        }
    }
    
}