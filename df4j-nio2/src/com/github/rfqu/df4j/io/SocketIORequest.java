package com.github.rfqu.df4j.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.rfqu.df4j.core.Link;

public class SocketIORequest extends Link 
implements CompletionHandler<Integer, AsyncSocketChannel> {
    static AtomicInteger ids=new AtomicInteger(); // DEBUG
    
    public int id=ids.addAndGet(1);
    protected AsyncSocketChannel channel;
    protected ByteBuffer buffer;
    protected boolean readOp;
    protected volatile boolean inTrans=false;
    
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
    
    public ByteBuffer getBuffer() {
        return buffer;
    }

    public void clear() {
        buffer.clear();
    }

    public void startRead() {
        if (inTrans) {
            throw new IllegalStateException("SocketIORequest.read: in "+(readOp?"read":"write")+" already");
        }
        inTrans=true;
        readOp=true;
        buffer.clear();
    }


    public void startWrite() {
        if (inTrans) {
            throw new IllegalStateException("SocketIORequest.write: in "+(readOp?"read":"write")+" already");
        }
        inTrans=true;
        readOp=false;
        buffer.flip();
    }

    @Override
    public void completed(Integer result, AsyncSocketChannel channel) {
        inTrans=false;
        if (readOp) {
            //System.out.println("channel read completed id="+id);
            buffer.flip();
            readCompleted(result, channel);
        } else {
            //System.out.println("channel write completed id="+id);
            buffer.clear();
            writeCompleted(result, channel);
        }
    }

    @Override
    public void failed(Throwable exc, AsyncSocketChannel channel) {
        inTrans=false;
        if (readOp) {
            readFailed(exc, channel);
        } else {
            writeFailed(exc, channel);
        }
    }

    /* to be overwritten */
    
    public void readCompleted(Integer result, AsyncSocketChannel channel) {
    }

    public void readFailed(Throwable exc, AsyncSocketChannel channel) {
    }

    public void writeCompleted(Integer result, AsyncSocketChannel channel) {
    }

    public void writeFailed(Throwable exc, AsyncSocketChannel channel) {
    }
}
 