package com.github.rfqu.df4j.io;

import java.nio.ByteBuffer;

import com.github.rfqu.df4j.core.Link;

public abstract class SocketIORequest extends Link {
    protected AsyncSocketChannel channel;
    protected ByteBuffer buffer;
    protected boolean readOp;
    protected boolean inTrans=false;
    public Integer result;
    public Throwable exc;
    
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
    
    public void clear() {
        buffer.clear();
        result=null;
        exc=null;
    }

    void requestStarted(AsyncSocketChannel channel, boolean readOp) {
        if (inTrans) {
            throw new IllegalStateException("SocketIORequest.read: in "+(readOp?"read":"write")+" already");
        }
        inTrans=true;
        this.readOp=readOp;
        this.channel=channel;
        buffer.clear();
    }

    protected void requestCompleted(Integer result) {
        if (!inTrans) {
            throw new IllegalStateException("SocketIORequest "+(readOp?"read":"write")+" completed but not in trans");
        }
        inTrans=false;
        channel=null;
        buffer.flip();
        if (readOp) {
            readCompleted(result);
        } else {
            writeCompleted(result);
        }
    }

    protected void writeCompleted(Integer result) {
        this.result=result;
    }

    protected void readCompleted(Integer result) {
        this.result=result;
    }

    protected void requestFailed(Throwable exc) {
        if (!inTrans) {
            throw new IllegalStateException("SocketIORequest "+(readOp?"read":"write")+" failed but not in trans");
        }
        inTrans=false;
        if (readOp) {
            readFailed(exc);
        } else {
            writeFailed(exc);
        }
    }

    protected void writeFailed(Throwable exc) {
        this.exc=exc;
    }

    protected void readFailed(Throwable exc) {
        this.exc=exc;
    }

}
