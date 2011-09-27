package com.github.rfqu.df4j.io;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import com.github.rfqu.df4j.core.Actor;

public class AsyncSocketChannel extends AsyncChannel {    
    protected SocketChannel channel;
    protected boolean connected=false;
    protected Throwable connectionFailure=null;
    protected RequestQueue readRequests=new RequestQueue(true);
    protected RequestQueue writeRequests=new RequestQueue(false);

    protected AsyncSocketChannel() throws IOException {
        super();
    }

    /**
     * for client-side socket
     * @throws IOException
     */
    public void connect(SocketAddress remote) throws IOException {
        SocketChannel channel = SocketChannel.open(remote);
        channel.configureBlocking(false);
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
     * for server-side socket
     * finishes connection
     * @throws IOException
     */
    protected void connCompleted(SocketChannel channel) {
        this.channel=channel;
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

    protected void read(SocketIORequest request) {
        request.startExchange(this, true);
        readRequests.send(request);
    }

    protected void write(SocketIORequest request) {
        request.startExchange(this, false);
        writeRequests.send(request);
    }

    @Override
    public void notify(SelectionKey key) {
        if (key.isReadable()) {
            readRequests.setReady(true);
        }
        if (key.isWritable()) {
            writeRequests.setReady(true);
        }
    }

    @Override
    public SocketChannel getChannel() {
        return channel;
    }

    class RequestQueue extends Actor<SocketIORequest> {
        final boolean read;
        int interestBit;

        public RequestQueue(boolean read) {
            this.read = read;
            interestBit = read? SelectionKey.OP_READ: SelectionKey.OP_WRITE;
        }

        @Override
        protected void act(SocketIORequest request) {
            try {
                int res=read? channel.read(request.buffer): channel.write(request.buffer);
                if (res == 0) {
                    input.push(request);
                    setReady(false);
                    interestOn(interestBit);
                } else {
                    request.requestCompleted(res);
                }
            } catch (Exception e) {
                request.requestFailed(e);
            }
        }
    }

}
