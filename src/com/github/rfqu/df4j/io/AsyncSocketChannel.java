package com.github.rfqu.df4j.io;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutorService;

import com.github.rfqu.df4j.core.Actor;
import com.github.rfqu.df4j.core.MessageQueue;
import com.github.rfqu.df4j.core.Port;

public abstract class AsyncSocketChannel extends AsyncChannel {
    protected AsynchronousSocketChannel channel;
    protected boolean connected;
    protected Port<AsyncSocketChannel> connectionCallback;
    protected RequestQueue readRequests=new RequestQueue();
    protected RequestQueue writeRequests=new RequestQueue();
    protected Throwable connectionFailure=null;

    /** for server-side socket
     * 
     * @param ch
     */
    public AsyncSocketChannel(AsynchronousSocketChannel ch) {
        this.channel=ch;
        connected=true;
        readRequests.poll(); // to unlock only
        writeRequests.poll();
    }

    /**
     * for client-side socket
     * needs subsequent call to connect(SocketAddress)
     * @throws IOException
     */
    public AsyncSocketChannel() throws IOException {
        ExecutorService executor=Actor.getCurrentExecutor();
        AsynchronousChannelGroup acg=getGroup(executor);
        AsynchronousSocketChannel ch=AsynchronousSocketChannel.open(acg);
        this.channel=ch;
        connected=false;
    }

    public void connect(SocketAddress remote, Port<AsyncSocketChannel> connectionCallback) {
        this.connectionCallback=connectionCallback;
        channel.connect(remote, this, connCompletion);
    }

    public void connect(SocketAddress remote) {
        connect(remote, null);
    }

    protected void connCompleted() {
        if (connectionCallback!=null) {
            connectionCallback.send(this);
        }
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

    public void read(SocketIORequest request, Port<SocketIORequest> callback) {
        if (request==null) {
            throw new NullPointerException();
        }
        request.readStart(this, callback);
        synchronized (this) {
            if (connectionFailure!=null) {
                throw new ConnectionException(connectionFailure);
            }
            request=readRequests.enqueueIfLocked(request);
        }
        channelRead(request);
    }

    public void read(SocketIORequest request) {
        read(request, null);
    }
    
    public void write(SocketIORequest request, Port<SocketIORequest> callback) {
        if (request==null) {
            throw new NullPointerException();
        }
        request.writeStart(this, callback);
        synchronized (this) {
            if (connectionFailure!=null) {
                throw new ConnectionException(connectionFailure);
            }
            request=writeRequests.enqueueIfLocked(request);
        }
        channelWrite(request);
    }

    public void write(SocketIORequest request) {
        write(request, null);
    }
    
    protected void requestCompleted(SocketIORequest request) {
        SocketIORequest nextRead=null;
        SocketIORequest nextWrite=null;
        synchronized (this) {
            if (request.readOp) {
                nextRead=readRequests.poll();
            } else {
                nextWrite=writeRequests.poll();
            }
        }
        channelRead(nextRead);
        channelWrite(nextWrite);
        Port<SocketIORequest> callback = request.callback;
        if (callback!=null) {
            callback.send(request);
        }
        if (request.readOp) {
            onReadCompleted(request);
        } else {
            onWriteCompleted(request);
        }
    }

    public void requestFailed(Throwable exc, SocketIORequest request) {
        // TODO Auto-generated method stub
        Port<SocketIORequest> callback = request.callback;
        if (callback!=null) {
            callback.send(request);
        }
        if (request.readOp) {
            onReadFailed(exc, request);
        } else {
            onWriteFailed(exc, request);
        }
    }

    public abstract void onReadCompleted(SocketIORequest request);

    public abstract void onWriteCompleted(SocketIORequest request);

    public abstract void onReadFailed(Throwable exc, SocketIORequest request);

    public abstract void onWriteFailed(Throwable exc, SocketIORequest request);

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
        channel.read(request.buf, request, requestCompletion);
    }

    private void channelWrite(SocketIORequest request) {
        if (request==null) {
            return;
        }
        try {
            channel.write(request.buf, request, requestCompletion);
        } catch (Exception e) {
            if (!channel.isOpen()) {
                return;
            }
            throw new ConnectionException(e);
            // TODO Auto-generated catch block
//            e.printStackTrace();
        }
    }

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
            request.requestCompleted(result);
        }
        @Override
        public void failed(Throwable exc, SocketIORequest request) {
            request.requestFailed(exc);
        }
    };
}
