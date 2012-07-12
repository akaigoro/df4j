package com.github.rfqu.df4j.ioexample;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import com.github.rfqu.df4j.core.SerialExecutor;
import com.github.rfqu.df4j.ioexample.EchoServer.Request;
import com.github.rfqu.df4j.nio2.AsyncSocketChannel;
import com.github.rfqu.df4j.nio2.SocketIOHandler;

class ServerConnection {
    private final EchoServer echoServer;
    AsyncSocketChannel channel;
    public int id;
    SerialExecutor serex = new SerialExecutor();
    private ByteBuffer buffer;
    Request request;
    boolean closed = false;

    public ServerConnection(EchoServer echoServer, AsyncSocketChannel channel) throws ClosedChannelException {
        this.echoServer = echoServer;
        this.channel = channel;
        this.id=echoServer.ids.addAndGet(1);
        buffer = ByteBuffer.allocate(EchoServer.BUF_SIZE);
        request = new EchoServer.Request(buffer);
        buffer.putInt(id);
        channel.write(request, endWrite1);
    }

    public void close() throws IOException {
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
        }
        try {
			channel.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			echoServer.conncClosed(this);
		}
    }

    SocketIOHandler<Request> endWrite1 = new SocketIOHandler<Request>(serex) {
        @Override
        protected void completed(int result, Request request) throws Exception {
            channel.read(request, endRead);
        }
    };
    
    SocketIOHandler<Request> endRead = new SocketIOHandler<Request>(serex) {
        @Override
        protected void completed(int result, Request request) {
            // System.out.println("  ServerRequest readCompleted id="+id);
            // read client's message as if all the data have been read
            buffer.position(buffer.limit());
            // write it back
            try {
                channel.write(request, endWrite);
            } catch (ClosedChannelException e) {
            }
        }

        @Override
        protected void closed(Request request) throws IOException {
            ServerConnection.this.close();
        }
    };

    SocketIOHandler<Request> endWrite = new SocketIOHandler<Request>(serex) {
        @Override
        protected void completed(int result, Request request) throws IOException {
            // System.out.println("  ServerRequest writeCompleted id="+id);
            try {
                // System.out.println("  ServerRequest read started id="+id);
                channel.read(request, endRead);
            } catch (ClosedChannelException e) {
            	closed(request);
            }
        }

        @Override
        protected void closed(Request request) throws IOException {
            ServerConnection.this.close();
        }
    };

}