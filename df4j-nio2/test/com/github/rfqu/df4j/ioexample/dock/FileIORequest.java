package com.github.rfqu.df4j.ioexample.dock;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;

import com.github.rfqu.df4j.core.Link;
import com.github.rfqu.df4j.core.Port;

public class FileIORequest extends Action<FileIORequest, Page> {
    protected long position;
    protected int length;
    protected Port<FileIORequest> callback;
    protected Integer result=null;
    protected Throwable exc=null;

    @Override
    public void act(Page page) {
        // TODO Auto-generated method stub
        
    }
    
    public void clear() {
        result=null;
        exc=null;
    }

    public void read(long position, Port<FileIORequest> callback) { 
        this.position=position;
        this.callback=callback;
        channel.read(buf, position, this, this);
    }

    public void write(long position, Port<FileIORequest> callback) { 
        this.position=position;
        this.callback=callback;
        channel.write(buf, position, this, this);
    }

    @Override
    public void completed(Integer result, FileIORequest attachment) {
        this.result=result;
        callback.send(this);
    }

    @Override
    public void failed(Throwable exc, FileIORequest attachment) {
        this.exc=exc;
        callback.send(this);
    }

    public AsynchronousFileChannel getChannel() {
        return channel;
    }

    public FileIORequest setBuffer(ByteBuffer buf) {
        this.buf = buf;
        return this;
    }

    public ByteBuffer getBuffer() {
        return buf;
    }

    public Port<FileIORequest> getCallback() {
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
