package com.github.rfqu.df4j.io;

import java.nio.ByteBuffer;

import com.github.rfqu.df4j.core.Link;

public class SocketIORequest extends Link {
    protected AsyncSocketChannel channel;
    protected ByteBuffer buffer;
    private boolean readOp;
    Integer result;
    Throwable exc;
    
    public SocketIORequest(int capacity, boolean direct) {
        if (direct) {
            buffer=ByteBuffer.allocateDirect(capacity);
        } else {
            buffer=ByteBuffer.allocate(capacity);
        }
    }
    
    public SocketIORequest(ByteBuffer buf) {
        this.buffer = buf;
    }

    public boolean isReadOp() {
        return readOp;
    }
    
    public AsyncSocketChannel getChannel() {
        return channel;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public void clear() {
        buffer.clear();
        result=null;
        exc=null;
    }

    void start(AsyncSocketChannel channel, boolean readOp) {
        if (this.channel!=null) {
            throw new IllegalStateException("SocketIORequest.read: in "+(readOp?"read":"write")+" already");
        }
        this.channel=channel;
        this.readOp=readOp;
        buffer.clear();
    }

    protected void completed(Integer result) {
        if (channel==null) {
            // TODO
            throw new IllegalStateException("SocketIORequest "+(readOp?"read":"write")+" completed but not in trans");
        }
        this.result=result;
        this.exc=null;
        buffer.flip();
        AsyncSocketChannel ch = channel;
        channel=null;
        ch.requestCompleted(this);
    }

    protected void failed(Throwable exc) {
        if (this.channel==null) {
            // TODO
            throw new IllegalStateException("SocketIORequest "+(readOp?"read":"write")+" failed but not in trans");
        }
        this.result=null;
        this.exc=exc;
        buffer.flip();
        AsyncSocketChannel ch = channel;
        channel=null;
        ch.requestCompleted(this);
    }

    public Integer getResult() {
        return result;
    }

    public Throwable getExc() {
        return exc;
    }
}
