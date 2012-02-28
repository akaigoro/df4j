package com.github.rfqu.df4j.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;

import com.github.rfqu.df4j.core.Link;

public class FileIORequest extends Link 
implements CompletionHandler<Integer, AsyncFileChannel> {
    protected long position;
    protected ByteBuffer buffer;
    protected boolean readOp;
    protected volatile boolean inTrans=false;

    public FileIORequest(int capacity, boolean direct) {
        if (direct) {
            buffer=ByteBuffer.allocateDirect(capacity);
        } else {
            buffer=ByteBuffer.allocate(capacity);
        }
    }
    
    public FileIORequest(ByteBuffer buf) {
        this.buffer = buf;
    }
    
    public void clear() {
        if (inTrans) {
            throw new IllegalStateException("FileIORequest.write: in "+(readOp?"read":"write")+" already");
        }
        if (buffer!=null) {
            buffer.clear();
        }
    }

    public void startRead(long position) { 
        if (inTrans) {
            throw new IllegalStateException("FileIORequest.write: in "+(readOp?"read":"write")+" already");
        }
        buffer.clear();
        inTrans=true;
        readOp=true;
        this.position=position;
    }

    public void startWrite(long position) { 
        if (inTrans) {
            throw new IllegalStateException("FileIORequest.write: in "+(readOp?"read":"write")+" already");
        }
        buffer.flip();
        inTrans=true;
        readOp=false;
        this.position=position;
    }

    @Override
    public void completed(Integer result, AsyncFileChannel channel) {
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
    public void failed(Throwable exc, AsyncFileChannel channel) {
        inTrans=false;
        if (exc instanceof AsynchronousCloseException) {
            try {
                channel.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if (readOp) {
            readFailed(exc, channel);
        } else {
            writeFailed(exc, channel);
        }
    }

    public FileIORequest setBuffer(ByteBuffer buf) {
        this.buffer = buf;
        return this;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public long getPosition() {
        return position;
    }
    
    /* to be overwritten */
    
    public void readCompleted(Integer result, AsyncFileChannel channel) {
    }

    public void readFailed(Throwable exc, AsyncFileChannel channel) {
    }

    public void writeCompleted(Integer result, AsyncFileChannel channel) {
    }

    public void writeFailed(Throwable exc, AsyncFileChannel channel) {
    }

}
