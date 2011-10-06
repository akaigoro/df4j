package com.github.rfqu.df4j.io;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SelectionKey;
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
    public void connect(AsyncServerSocketChannel s) {//throws IOException {
        s.getChannel().accept(this, acceptCompletion);        
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
        startRequest(nextRead);
        startRequest(nextWrite);
    }

    protected synchronized void connFailed(Throwable exc) {
        connectionFailure=exc;
        // TODO fail enqueued requests
    }

    public void read(SocketIORequest request) {
        if (connectionFailure!=null) {
            throw new ConnectionException(connectionFailure);
        }
        request.start(this, true);
        synchronized (this) {
            request=readRequests.enqueueIfLocked(request);
        }
        startRequest(request);
    }

    public void write(SocketIORequest request) {
        if (connectionFailure!=null) {
            throw new ConnectionException(connectionFailure);
        }
        request.start(this, false);
        synchronized (this) {
            request=writeRequests.enqueueIfLocked(request);
        }
        startRequest(request);
    }

    private void startRequest(SocketIORequest request) {
        if (request==null) {
            return;
        }
        if (request.isReadOp()) {
            channel.read(request.buffer, request, requestCompletion);
        } else {
            channel.write(request.buffer, request, requestCompletion);
        }
    }

    protected void requestCompleted(SocketIORequest request) {
        SocketIORequest next;
        synchronized (this) {
            if (request.isReadOp()) {
                next=readRequests.poll();
            } else {
                next=writeRequests.poll();
            }
        }
        startRequest(next);
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
            try {
                request.completed(result);
            } catch (Exception e) { // this is the first method call on stack, nobody to report the exception
                // TODO 
                e.printStackTrace();
            }
        }
        @Override
        public void failed(Throwable exc, SocketIORequest request) {
            try {
                request.failed(exc);
            } catch (Exception e) { // this is the first method call on stack, nobody to report the exception
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    };

}
