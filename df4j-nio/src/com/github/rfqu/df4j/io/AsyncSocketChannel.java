package com.github.rfqu.df4j.io;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import com.github.rfqu.df4j.core.Actor;

public class AsyncSocketChannel extends AsyncChannel {    
    protected SocketChannel channel;
    protected Throwable connectionFailure=null;
    protected RequestQueue readRequests=new RequestQueue(true);
    protected RequestQueue writeRequests=new RequestQueue(false);

    protected AsyncSocketChannel() throws IOException {
    }

    /**
     * for client-side socket
     * @throws IOException
     */
    public void connect(SocketAddress remote) throws IOException {
        SocketChannel channel = SocketChannel.open(remote);
        connCompleted(channel);
    }

    /**
     * for server-side socket
     * start connection, to be continued in connCompleted()
     * @throws ClosedChannelException 
     * @throws IOException
     */
    public void connect(AsyncServerSocketChannel s) throws IOException {
        s.accept(this);        
    }

    /**
     * for all types of sockets
     * finishes connection
     * @throws IOException
     */
    protected void connCompleted(SocketChannel channel) throws IOException {
        this.channel=channel;
        channel.configureBlocking(false);
        interestOn(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        readRequests.setReady(true);
        writeRequests.setReady(true);
    }

    /**
     * for server-side socket
     * finishes connection
     */
    protected synchronized void connFailed(Throwable exc) {
        connectionFailure=exc;
        // TODO fail enqueued requests
    }

    public void read(SocketIORequest request) {
        request.start(this, true);
        readRequests.send(request);
    }

    public void write(SocketIORequest request) {
        request.start(this, false);
        writeRequests.send(request);
    }

    @Override
    synchronized void notify(SelectionKey key) {
        if (channel==null) {
            return;
        }
        if (key.isReadable()) {
            readRequests.setReady(true);
        }
        if (key.isWritable()) {
            writeRequests.setReady(true);
        }
    }

    protected void requestCompleted(SocketIORequest request) {
    }

    @Override
    public SocketChannel getChannel() {
        return channel;
    }

    public synchronized void close() throws IOException {
        if (channel != null) {
            channel.close();
            channel=null;
        }
    }

    class RequestQueue extends Actor<SocketIORequest> {
        final boolean read;

        public RequestQueue(boolean read) {
            this.read = read;
        }

        @Override
        protected void act(SocketIORequest request) {
            try {
                int res=read? channel.read(request.buffer): channel.write(request.buffer);
                if (res == 0) {
                    synchronized (this) {
                        input.push(request);
                        ready=false;
                    }
                } else {
                    request.completed(res);
                }
            } catch (Exception e) {
                request.failed(e);
            }
        }
    }

}
