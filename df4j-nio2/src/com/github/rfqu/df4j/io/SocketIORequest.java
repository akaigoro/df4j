package com.github.rfqu.df4j.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.rfqu.df4j.core.Link;
import com.github.rfqu.df4j.core.Port;

public class SocketIORequest extends Link 
implements CompletionHandler<Integer, AsyncSocketChannel> {
    static AtomicInteger ids=new AtomicInteger();
    
    public int id=ids.addAndGet(1);
    protected AsyncSocketChannel channel;
    protected ByteBuffer buffer;
    protected boolean readOp;
    protected boolean inTrans=false;
    protected Throwable exc;
    protected Port<SocketIORequest> callback;
    
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
    
    public Throwable getExc() {
        return exc;
    }

    public Port<SocketIORequest> getCallback() {
        return callback;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public void clear() {
        buffer.clear();
    }

    public void startRead(Port<SocketIORequest> callback) {
        if (inTrans) {
            throw new IllegalStateException("SocketIORequest.read: in "+(readOp?"read":"write")+" already");
        }
        exc=null;
        inTrans=true;
        readOp=true;
        this.callback = callback;
        buffer.clear();
    }


    public void startWrite(Port<SocketIORequest> callback) {
        if (inTrans) {
            throw new IllegalStateException("SocketIORequest.write: in "+(readOp?"read":"write")+" already");
        }
        inTrans=true;
        exc=null;
        readOp=false;
        this.callback = callback;
        buffer.flip();
    }

    @Override
    public void completed(Integer result, AsyncSocketChannel channel) {
        inTrans=false;
        if (readOp) {
            //System.out.println("channel read completed id="+id);
            buffer.flip();
            channel.readCompleted();
        } else {
            //System.out.println("channel write completed id="+id);
            channel.writeCompleted();
            buffer.clear();
        }
        if (callback!=null) {
            callback.send(this);
        }
    }

    @Override
    public void failed(Throwable exc, AsyncSocketChannel channel) {
        this.exc=exc;
        inTrans=false;
        if (exc instanceof AsynchronousCloseException && channel!=null) {
            try {
                channel.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if (readOp) {
            channel.readCompleted();
        } else {
            channel.writeCompleted();
        }
        if (callback!=null) {
            callback.send(this);
        }
    }
}
 