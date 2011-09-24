package com.github.rfqu.df4j.io;

import java.nio.ByteBuffer;

import com.github.rfqu.df4j.core.Link;
import com.github.rfqu.df4j.core.Port;

public class SocketIORequest extends Link {
    protected AsyncSocketChannel channel;
    protected Port<SocketIORequest> callback;
    protected ByteBuffer buf;
    protected boolean readOp;
    protected Integer result=null;
    protected Throwable exc=null;
    
    public SocketIORequest() {
    }
    
    public SocketIORequest(ByteBuffer buf) {
        this.buf = buf;
    }
    
    public void clear() {
        exc=null;
        if (buf!=null) {
            buf.clear();
        }
    }

    void readStart(AsyncSocketChannel channel, Port<SocketIORequest> callback) {
        readOp=true;
        this.channel=channel;
        this.callback=callback;
        buf.clear();
    }


    public void writeStart(AsyncSocketChannel channel, Port<SocketIORequest> callback) {
        readOp=false;
        this.channel=channel;
        this.callback=callback;
        buf.flip();
    }

    public void requestCompleted(Integer result) {
        this.result=result;
        buf.flip();
        channel.requestCompleted(this);
        if (callback!=null) {
            callback.send(this);
        }
    }

    public void requestFailed(Throwable exc) {
        this.exc=exc;
        channel.requestFailed(exc, this);
        if (callback!=null) {
            callback.send(this);
        }
    }

    public ByteBuffer getBuffer() {
        return buf;
    }

    public boolean isReadOp() {
        return readOp;
    }

    public Port<SocketIORequest> getCallback() {
        return callback;
    }

    public Throwable getExc() {
        return exc;
    }

    public AsyncSocketChannel getChannel() {
        return channel;
    }
}
