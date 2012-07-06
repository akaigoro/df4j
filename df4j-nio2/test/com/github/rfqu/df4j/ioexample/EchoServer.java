package com.github.rfqu.df4j.ioexample;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.rfqu.df4j.core.SerialExecutor;
import com.github.rfqu.df4j.nio2.AsyncServerSocketChannel;
import com.github.rfqu.df4j.nio2.AsyncSocketChannel;
import com.github.rfqu.df4j.nio2.SocketIOHandler;
import com.github.rfqu.df4j.nio2.SocketIORequest;

public class EchoServer extends AsyncServerSocketChannel {
    static final int BUF_SIZE = 128;
    static AtomicInteger ids=new AtomicInteger(); // DEBUG
    
    ArrayList<Connection> connections=new ArrayList<Connection>();
    
    public EchoServer(InetSocketAddress addr) throws IOException {
        super(addr);
        welcome();
    }

    /** prepare connection handler for future connections 
     */
    private synchronized void welcome() throws ClosedChannelException {
        AsyncSocketChannel channel=new AsyncSocketChannel();
        this.send(channel);
        Connection connection = new Connection(channel);
        connections.add(connection);
//        System.out.println("connections="+connections.size());
    }

    /** Accept client connection request.
     *  Prepare new ready Connection object.
     */
    @Override
    public void completed(AsynchronousSocketChannel result, AsyncSocketChannel channel) {
        super.completed(result, channel);
        //  System.out.println("  Server connection accepted");
        // prepare another connection handler for future connections
        try {
            welcome();
        } catch (ClosedChannelException e) {
        }
    }

    public synchronized void close() {
        for (;;) {
            int sz=connections.size();
            if (sz==0) break;
            try {
                connections.get(sz-1).close(); // removes from the collection
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try {
        	super.complete();
            super.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    class Connection implements Comparable<Connection>{
        
        public int id=ids.addAndGet(1);
        AsyncSocketChannel channel;
        SerialExecutor serex=new SerialExecutor();
        private ByteBuffer buffer;
        boolean closed=false;

        public Connection(AsyncSocketChannel channel) throws ClosedChannelException {
            this.channel=channel;
            buffer=ByteBuffer.allocate(EchoServerTest.BUF_SIZE);
            startRead(channel);
        }

		private void startRead(AsyncSocketChannel channel) throws ClosedChannelException {
//            System.out.println("  ServerRequest read started id="+id);
			channel.read(buffer, endRead);
		}

        public void close() throws IOException {
            closed=true;
            channel.close();
            synchronized(EchoServer.this) {
                connections.remove(Connection.this);
            }
//            System.out.println("connections="+connections.size());
        }

        SocketIOHandler endRead = new SocketIOHandler(serex) {
            @Override
            protected void completed(Integer result, SocketIORequest request) {
                // System.out.println("  ServerRequest readCompleted id="+id);
                // read client's message as if all the data have been read
                buffer.position(buffer.limit());
                // write it back
                try {
                    channel.write(request.getBuffer(), endWrite);
                } catch (ClosedChannelException e) {
                }
            }

            @Override
            protected  void closed(SocketIORequest request) throws IOException {
                Connection.this.close();
            }
        };

        SocketIOHandler endWrite = new SocketIOHandler(serex) {
            @Override
            protected void completed(Integer result, SocketIORequest request) {
//              System.out.println("  ServerRequest writeCompleted id="+id);
                try {
                    startRead(channel);
                } catch (ClosedChannelException e) {
                }
            }

            @Override
            protected  void closed(SocketIORequest request) throws IOException {
                Connection.this.close();
            }
        };

        @Override
        public int compareTo(Connection o) {
            return id-o.id;
        }
        
    }
}