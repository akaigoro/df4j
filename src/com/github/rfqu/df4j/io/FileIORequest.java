package com.github.rfqu.df4j.io;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;

import com.github.rfqu.df4j.core.Link;
import com.github.rfqu.df4j.core.Port;

public class FileIORequest extends Link implements CompletionHandler<Integer, FileIORequest> {
    protected AsynchronousFileChannel channel;
    protected long position;
    protected Port<FileIORequest> callback;
    protected ByteBuffer buf;
    protected Integer result=null;
    protected Throwable exc=null;

    public FileIORequest(AsynchronousFileChannel channel) {
        this.channel = channel;
    }
    
    public FileIORequest(AsynchronousFileChannel channel, ByteBuffer buf) {
        this.channel = channel;
        this.buf = buf;
    }
    
    public void clear() {
        result=null;
        exc=null;
        if (buf!=null) {
            buf.clear();
        }
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
