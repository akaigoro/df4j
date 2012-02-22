package com.github.rfqu.df4j.io;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;

import com.github.rfqu.df4j.core.Actor;
import com.github.rfqu.df4j.core.Link;
import com.github.rfqu.df4j.core.Port;

public class AsyncSocketChannel extends Link
implements  CompletionHandler<AsynchronousSocketChannel,AsyncServerSocketChannel> {
    protected AsynchronousSocketChannel channel;
    protected boolean connected=false;
    protected boolean closed=false;
    protected Throwable connectionFailure=null;
    protected RequestQueue readRequests=new RequestQueue();
    protected RequestQueue writeRequests=new RequestQueue();

    /**
     * for client-side socket
     * @throws IOException
     */
    public void connect(SocketAddress remote) throws IOException {
        AsynchronousChannelGroup acg=AsyncChannelCroup.getCurrentACGroup();
        this.channel=AsynchronousSocketChannel.open(acg);
        channel.connect(remote, null, new CompletionHandler<Void, Object>() {
            @Override
            public void completed(Void result, Object attachment) {
                connected=true;
                readRequests.start();
                writeRequests.start();
            }
            @Override
            public void failed(Throwable exc, Object attachment) {
                connectionFailure=exc;
            } 
        });
    }

    /**
     * For server-side socket.
     * Callback for successful acceptance of incoming connection.
     * First do AsyncServerSocketChannel.send(this);
     */
    @Override
    public void completed(AsynchronousSocketChannel result, AsyncServerSocketChannel attachment) {
        channel=result;
        connected=true;
        readRequests.start();
        writeRequests.start();
        attachment.acceptCompleted();
    }

    /**
     * For server-side socket.
     * Callback for failed acceptance of incoming connection.
     * First do AsyncServerSocketChannel.send(this);
     */
    @Override
    public void failed(Throwable exc, AsyncServerSocketChannel attachment) {
        connectionFailure=exc;
        attachment.acceptCompleted();
    }

    public void read(SocketIORequest request, Port<SocketIORequest> callback) throws ClosedChannelException {
        checkRequest(request);
        request.startRead(callback);
        readRequests.send(request);
    }

    public void write(SocketIORequest request, Port<SocketIORequest> callback) throws ClosedChannelException {
        checkRequest(request);
        request.startWrite(callback);
        writeRequests.send(request);
    }

    protected void checkRequest(SocketIORequest request) throws ClosedChannelException {
        if (request==null) {
            throw new IllegalArgumentException(connectionFailure);
        }
        if (connectionFailure!=null) {
            throw new IllegalStateException(connectionFailure);
        }
        if (closed) {
            throw new ClosedChannelException();
        }
    }

    /** read request completed, channel is free
     */
    protected void readCompleted() {
        readRequests.start();
    }

    /** read request completed, channel is free
     */
    protected void writeCompleted() {
        writeRequests.start();
    }

    class RequestQueue extends Actor<SocketIORequest> {

        @Override
        protected void act(SocketIORequest request) throws Exception {
            if (closed) {
                request.failed(new AsynchronousCloseException(), null);
            }
            stop();
            if (request.readOp) {
                //System.out.println("channel read started id="+request.id);
                channel.read(request.buffer, AsyncSocketChannel.this, request);
            } else {
                //System.out.println("channel write started id="+request.id +" limit="+request.buffer.limit());
                channel.write(request.buffer, AsyncSocketChannel.this, request);
            }
        }
        @Override
        protected void complete() throws Exception {
            // TODO Auto-generated method stub
        }
    }
    
    public AsynchronousSocketChannel getChannel() {
        return channel;
    }

    public void close() throws IOException {
        closed=true;
        readRequests.start();
        writeRequests.start();
        channel.close();
    }

}
