package com.github.rfqu.df4j.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;

import org.junit.Test;

import com.github.rfqu.df4j.core.Callback;
import com.github.rfqu.df4j.core.CallbackFuture;
import com.github.rfqu.df4j.core.Port;
import com.github.rfqu.df4j.nio.AsyncServerSocketChannel;
import com.github.rfqu.df4j.nio.AsyncSocketChannel;
import com.github.rfqu.df4j.nio.SocketIORequest;

public abstract class AsyncServerSocketChannelTest {
    static final int BUF_SIZE = 128;
    static final InetSocketAddress local9990 = new InetSocketAddress("localhost", 9990);

    AsyncChannelFactory asyncrSocketFactory;
    
    protected Connection newConnection(InetSocketAddress addr) throws IOException {
        return new Connection(asyncrSocketFactory.newAsyncSocketChannel(addr));
    }


    /**
     * tests that overall connection count can be more than maxConn
     */
    @Test
    public void maxConnTest() throws Exception {
        final int maxConn0=2;
        Server server=new Server(local9990);
        
        ArrayList<Connection>allConns=new ArrayList<Connection>();
        server.assch.up(maxConn0);
        int delta=maxConn0/2;
        int clConns=maxConn0+delta;
        for (int k=0; k<clConns; k++) {
            Connection conn = newConnection(local9990);
            allConns.add(conn);
            conn.get();
        }
        Thread.sleep(100); // wait server to accept connections
        // only maxConn0 should be opened
        assertEquals(maxConn0, server.channelCounter);

        // the rest opened
        server.assch.up(delta);
        
        Thread.sleep(100); // wait server to accept connections
        assertEquals(clConns, server.channelCounter);
        assertTrue(server.allOpened);
        server.close();
    }

    /**
     * tests sending one packet from client to server
     */
    @Test
    public void clientToServer() throws Exception {
        Server server=new Server(local9990);
        server.assch.up();

        Connection clconn = newConnection(local9990);
//        clconn.get();
        Connection serconn = server.allConns.take();
        MyRequest clreq=new MyRequest();
        clreq.clear();
        final long value = 777;
        clreq.buff.putLong(value);
        clconn.write(clreq);
        MyRequest serreq=new MyRequest();
        serconn.read(serreq);
        assertEquals(serreq, serconn.finishedRequests.take());
        final long long1 = serreq.buff.getLong();
        assertEquals(value, long1);
        
        assertEquals(clreq, clconn.finishedRequests.take());

        server.close();
    }

    static class MyRequest extends SocketIORequest<MyRequest> {
        ByteBuffer buff;
        
        public MyRequest() {
            super(ByteBuffer.allocate(BUF_SIZE));
            buff=super.getBuffer();
        }
    
    }
    
    class Server implements Callback<AsyncSocketChannel> {        
        AsyncServerSocketChannel assch;    
        ArrayBlockingQueue<Connection> allConns=new ArrayBlockingQueue<Connection>(10);
        int channelCounter=0;
        boolean allOpened=true;
            
        public Server(InetSocketAddress addr) throws IOException {
            assch=asyncrSocketFactory.newAsyncServerSocketChannel(addr, this);
        }
        
        @Override
        public void post(AsyncSocketChannel channel) {
            try {
                channelCounter++;
                if (channel.isClosed()) {
                    allOpened=false;
                }
                allConns.add(new Connection(channel));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        /** closing requested */
        protected void close() throws Exception {
            assch.close();
        }

        @Override
        public void postFailure(Throwable exc) {
            exc.printStackTrace();
        }
    }

    static class Connection implements Port<MyRequest> {
        AsyncSocketChannel conn;
        CallbackFuture<AsyncSocketChannel> connListener=new CallbackFuture<AsyncSocketChannel>();
        ArrayBlockingQueue<MyRequest> finishedRequests=new ArrayBlockingQueue<MyRequest>(10);

        Connection(AsyncSocketChannel conn) throws IOException {
            this.conn=conn;
            conn.addConnListener(connListener);
        }

        public void read(MyRequest request) {
            conn.read(request);
            request.setListener(this);
        }

        public void write(MyRequest request) {
            conn.write(request);
            request.setListener(this);
        }

        public void get() throws InterruptedException, ExecutionException {
            connListener.get();
        }

        /** called when io operation completed */
        @Override
        public void post(MyRequest m) {
            finishedRequests.add(m);
        }
    }
    
}