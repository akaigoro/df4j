package com.github.rfqu.df4j.io;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutorService;

import com.github.rfqu.df4j.core.Actor;
import com.github.rfqu.df4j.core.MessageQueue;

public abstract class AsyncSocketChannel extends AsyncChannel {
    protected AsynchronousSocketChannel channel;
    protected boolean connected=false;
    protected Throwable connectionFailure=null;
    protected RequestQueue readRequests=new RequestQueue();
    protected RequestQueue writeRequests=new RequestQueue();

    /** for server-side socket
     * 
     * @param ch
     */
    public AsyncSocketChannel(AsynchronousSocketChannel ch) {
        this.channel=ch;
        readRequests.poll(); // to unlock only
        writeRequests.poll();
    }

    public AsyncSocketChannel() {
    }

    /**
     * for server-side socket
     * @throws IOException
     */
    public void accept(AsynchronousServerSocketChannel assch) {//throws IOException {
        assch.accept(this, acceptCompletion);        
    }

    /**
     * for client-side socket
     * @throws IOException
     */
    public void connect(SocketAddress remote) throws IOException {
        ExecutorService executor=Actor.getCurrentExecutor();
        AsynchronousChannelGroup acg=getGroup(executor);
        AsynchronousSocketChannel ch=AsynchronousSocketChannel.open(acg);
        this.channel=ch;
        channel.connect(remote, this, connCompletion);
    }

    protected void connCompleted() {
        SocketIORequest nextRead=null;
        SocketIORequest nextWrite=null;
        synchronized (this) {
            connected=true;
            nextRead=readRequests.poll();
            nextWrite=writeRequests.poll();
        }
        channelRead(nextRead);
        channelWrite(nextWrite);
    }

    protected synchronized void connFailed(Throwable exc) {
        connectionFailure=exc;
        // TODO fail enqueued requests
    }

    protected void read(SocketIORequest request) {
        synchronized (this) {
            if (connectionFailure!=null) {
                throw new ConnectionException(connectionFailure);
            }
            request=readRequests.enqueueIfLocked(request);
        }
        channelRead(request);
    }

    protected void write(SocketIORequest request) {
        synchronized (this) {
            if (connectionFailure!=null) {
                throw new ConnectionException(connectionFailure);
            }
            request=writeRequests.enqueueIfLocked(request);
        }
        channelWrite(request);
    }

    protected void requestCompleted(SocketIORequest request) {
        SocketIORequest next;
        synchronized (this) {
            if (request.readOp) {
                next=readRequests.poll();
            } else {
                next=writeRequests.poll();
            }
        }
        if (request.readOp) {
            channelRead(next);
        } else {
            channelWrite(next);
        }
    }

    static class RequestQueue extends MessageQueue<SocketIORequest> {
        private boolean locked=true;

        public SocketIORequest enqueueIfLocked(SocketIORequest request) {
            if (locked) {
                super.enqueue(request);
                return null;
            } else {
                locked=true;
                return request;
            }
        } 

        public SocketIORequest poll() {
            SocketIORequest res = super.poll();
            locked=(res!=null);
            return res;
        }
    }

    private void channelRead(SocketIORequest request) {
        if (request==null) {
            return;
        }
        channel.read(request.buffer, request, requestCompletion);
    }

    private void channelWrite(SocketIORequest request) {
        if (request==null) {
            return;
        }
        try {
            channel.write(request.buffer, request, requestCompletion);
        } catch (Exception e) {
            if (!channel.isOpen()) {
                return;
            }
            throw new ConnectionException(e);
            // TODO Auto-generated catch block
//            e.printStackTrace();
        }
    }
    
    public AsynchronousSocketChannel getChannel() {
        return channel;
    }

    public void close() throws IOException {
        channel.close();
    }

    static CompletionHandler<AsynchronousSocketChannel, AsyncSocketChannel> acceptCompletion
        = new CompletionHandler<AsynchronousSocketChannel, AsyncSocketChannel>()
    {
        @Override
        public void completed(AsynchronousSocketChannel result, AsyncSocketChannel asc) {
            asc.channel=result;
            asc.connCompleted();
        }
        @Override
        public void failed(Throwable exc, AsyncSocketChannel asc) {
            asc.connFailed(exc);
        }
    };

    static CompletionHandler<Void, AsyncSocketChannel> connCompletion =new CompletionHandler<Void, AsyncSocketChannel>() {
        @Override
        public void completed(Void result, AsyncSocketChannel asc) {
            asc.connCompleted();
        }
        @Override
        public void failed(Throwable exc, AsyncSocketChannel asc) {
            asc.connFailed(exc);
        }
    };

    static CompletionHandler<Integer, SocketIORequest> requestCompletion =new CompletionHandler<Integer, SocketIORequest>() {
        @Override
        public void completed(Integer result, SocketIORequest request) {
            request.channel.requestCompleted(request);
            request.requestCompleted(result);
        }
        @Override
        public void failed(Throwable exc, SocketIORequest request) {
            request.channel.requestCompleted(request);
            request.requestFailed(exc);
        }
    };

}
