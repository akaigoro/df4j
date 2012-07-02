package com.github.rfqu.df4j.ioexample;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.rfqu.df4j.core.AsyncHandler;
import com.github.rfqu.df4j.core.SerialExecutor;
import com.github.rfqu.df4j.nio2.AsyncServerSocketChannel;
import com.github.rfqu.df4j.nio2.AsyncSocketChannel;
import com.github.rfqu.df4j.nio2.SocketIORequest;

public class EchoServer extends AsyncServerSocketChannel {
    static final int BUF_SIZE = 128;
    
    HashSet<Connection> connections=new HashSet<Connection>();
    
    public EchoServer(InetSocketAddress addr, int connCount) throws IOException {
        super(addr);
        accept();
    }

    /** wait client to connect */
    private void accept() {
        AsyncSocketChannel channel=new AsyncSocketChannel();
        this.send(channel);
        Connection connection = new Connection(channel);
        connections.add(connection);
    }

    @Override
    public void completed(AsynchronousSocketChannel result, AsyncSocketChannel attachment) {
        super.completed(result, attachment);
        System.out.println("  Server connection accepted");
        accept(); // prepare another connection
    }

    public void close() {
        for (Connection connection: connections) {
            try {
                connection.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try {
            super.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    static class Connection {
        static AtomicInteger ids=new AtomicInteger(); // DEBUG
        
        public int id=ids.addAndGet(1);
        AsyncSocketChannel channel;
        SerialExecutor serex=new SerialExecutor();
        SocketIORequest request;
        private ByteBuffer buffer;

        public Connection(AsyncSocketChannel channel) {
            this.channel=channel;
            request = new SocketIORequest(BUF_SIZE, false);
            buffer=request.getBuffer();
            channel.read(request, endRead);
        }

        public void close() throws IOException {
            channel.close();
        }

        AsyncHandler<SocketIORequest> endRead = new AsyncHandler<SocketIORequest>(serex) {
            @Override
            protected void act(SocketIORequest request) throws Exception {
              System.out.println("  ServerRequest readCompleted id="+id);
                // read client's message
                // as if all the data read and written back
                buffer.position(buffer.limit());
                // write it back
                channel.write(request, endWrite);
            }
        };

        AsyncHandler<SocketIORequest> endWrite = new AsyncHandler<SocketIORequest>(serex) {

            @Override
            protected void act(SocketIORequest request) throws Exception {
                System.out.println("  ServerRequest writeCompleted id="+id);
                channel.read(request, endRead);
            }
            
        };
        
    }
}