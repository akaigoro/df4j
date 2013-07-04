package com.github.rfqu.df4j.nio.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.junit.Test;

import com.github.rfqu.df4j.core.Port;
import com.github.rfqu.df4j.nio.AsyncChannelFactory;
import com.github.rfqu.df4j.nio.AsyncSocketChannel;
import com.github.rfqu.df4j.nio.IOCallback;
import com.github.rfqu.df4j.nio.LimitedServer;
import com.github.rfqu.df4j.nio.SocketIORequest;

public abstract class LimitedServerTest {
    static final int BUF_SIZE = 128;
//    static final InetSocketAddress local9990 = new InetSocketAddress("localhost", 9990);
    static final InetSocketAddress local9990 = new InetSocketAddress("localhost", 8007);

    AsyncChannelFactory asyncrSocketFactory;
    
    /**
     * tests serever with max 1 connection
     */
    @Test
    public void smokeTest() throws Exception {
        Server server=new Server(local9990, 1, 1);
        Connection conn = newClientConnection(local9990);
        Thread.sleep(100); // wait server to accept connections
        // only maxConn0 should be opened
        assertEquals(1, server.allConns.size());
        Connection serverConn=server.allConns.get(0);
        assertTrue(serverConn.conn.getConnEvent().isDone());
        assertFalse(serverConn.conn.isClosed());

        MyRequest r=new MyRequest();
        int msg=12345;
        r.getBuffer().putInt(msg);
        conn.write(r);
        Thread.sleep(100); 
        assertEquals(1, serverConn.finishedRequests.size());

        int msg2=serverConn.finishedRequests.get(0).getBuffer().getInt();
        assertEquals(msg, msg2);
  
        conn.close();
        Thread.sleep(100); 
        assertTrue(serverConn.conn.isClosed());

        server.close();
    }

    /**
     * tests that overall connection count cannot be more than maxConn
    @Test
    public void maxConnTest() throws Exception {
        final int maxConn0=2;
        Server server=new Server(local9990, maxConn0);
        ArrayList<Connection> allConns=new ArrayList<Connection>();
        
        int delta=maxConn0/2;
        int clConns=maxConn0+delta;
        for (int k=0; k<clConns; k++) {
            Connection conn = newClientConnection(local9990);
            allConns.add(conn);
        }
        Thread.sleep(100); // wait server to accept connections
        // only maxConn0 should be opened
        assertEquals(maxConn0, server.channelCounter);

        Thread.sleep(100); // wait server to accept connections
        assertEquals(clConns, server.channelCounter);
        assertTrue(server.allOpened);
        server.close();
    }
     */

    private Connection newClientConnection(InetSocketAddress addr) throws IOException {
        AsyncSocketChannel asc=asyncrSocketFactory.newAsyncSocketChannel();
        asc.connect(addr);
        return new Connection(asc);
    }
    

    /**
     * tests sending one packet from client to server
    @Test
    public void clientToServer() throws Exception {
        Server server=new Server(local9990);
        server.assc.up();

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
     */

    static class MyRequest extends SocketIORequest<MyRequest> {
        ByteBuffer buff;
        
        public MyRequest() {
            super(ByteBuffer.allocate(BUF_SIZE));
            buff=super.getBuffer();
        }
    
    }
    
    static class Connection implements Port<MyRequest>, IOCallback<MyRequest> {
        AsyncSocketChannel conn;
        ArrayList<MyRequest> finishedRequests=new ArrayList<MyRequest>();

        Connection(AsyncSocketChannel conn) {
            this.conn=conn;
        }

        public void close() {
            try {
                conn.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        public void read(MyRequest request) {
            conn.read(request);
            request.setListener(this);
        }

        public void write(MyRequest request) {
            conn.write(request);
            request.setListener(this);
        }

        public void read() {
            MyRequest r=new MyRequest();
            this.read(r);           
        }

        //======== backend
        
        /** called when I/O operation have completed */
        @Override
        public void post(MyRequest request) {
            finishedRequests.add(request);
            request.toIOCallback(this);
        }

        @Override
        public void completed(int result, MyRequest request) {
            read();
        }

        @Override
        public void timedOut(MyRequest request) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void closed(MyRequest request) {
            close();
        }

        @Override
        public void failed(Throwable exc, MyRequest request) {
            // TODO Auto-generated method stub
            
        }
    }

    class Server extends LimitedServer {        
        ArrayList<Connection> allConns=new ArrayList<Connection>();
            
        public Server(InetSocketAddress addr, int waitCount, int maxCount) throws IOException {
            super(asyncrSocketFactory, addr);
            super.start(waitCount, maxCount);
        }

        @Override
        protected void accepted(AsyncSocketChannel asc) {
            Connection conn = new Connection(asc);
            allConns.add(conn);
            conn.read();
        }
    }
}