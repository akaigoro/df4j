package com.github.rfqu.df4j.ioexample.dock;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;

import com.github.rfqu.df4j.core.Port;

public class Page implements CompletionHandler<Integer, Page> {
    int key;
    
    public void init(int key) {
        this.key=key;
    }

    
    protected AsynchronousFileChannel channel;
    protected ByteBuffer buf;
    protected Integer result=null;
    protected Throwable exc=null;

    public Page(AsynchronousFileChannel channel) {
        this.channel = channel;
    }
    
    public Page(AsynchronousFileChannel channel, ByteBuffer buf) {
        this.channel = channel;
        this.buf = buf;
    }
    
    public Page() {
        // TODO Auto-generated constructor stub
    }

    public void clear() {
        result=null;
        exc=null;
        if (buf!=null) {
            buf.clear();
        }
    }
    public void read(long position, Port<Page> callback) { 
        this.position=position;
        this.callback=callback;
        channel.read(buf, position, this, this);
    }

    public void write(long position, Port<Page> callback) { 
        this.position=position;
        this.callback=callback;
        channel.write(buf, position, this, this);
    }

    @Override
    public void completed(Integer result, Page attachment) {
        this.result=result;
        callback.send(this);
    }

    @Override
    public void failed(Throwable exc, Page attachment) {
        this.exc=exc;
        callback.send(this);
    }

    public AsynchronousFileChannel getChannel() {
        return channel;
    }

    public Page setBuffer(ByteBuffer buf) {
        this.buf = buf;
        return this;
    }

    public ByteBuffer getBuffer() {
        return buf;
    }

    public Port<Page> getCallback() {
        return callback;
    }

    public long getPosition() {
        return position;
    }
    
    public Integer getResult() {
        return result;
    }

    public Throwable getExc() {
        return exc;
    }
    
}
