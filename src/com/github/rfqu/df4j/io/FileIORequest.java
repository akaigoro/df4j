package com.github.rfqu.df4j.io;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;

import com.github.rfqu.df4j.core.Port;

public class FileIORequest extends IORequest<FileIORequest> {
    protected AsynchronousFileChannel channel;
    protected long position;
    
    public FileIORequest(AsynchronousFileChannel channel) {
        this.channel = channel;
    }
    
    public FileIORequest(AsynchronousFileChannel channel, ByteBuffer buf) {
        this.channel = channel;
        this.buf = buf;
    }
    
    public void read(ByteBuffer buf, long position, Port<FileIORequest> callback) { 
        this.buf=buf;
        read(position, callback);
    }

    public void read(long position, Port<FileIORequest> callback) { 
        this.position=position;
        this.callback=callback;
        channel.read(buf, position, this, this);
    }

    public void write(ByteBuffer buf, long position, Port<FileIORequest> callback) { 
        this.buf = buf;
        write(position, callback);
    }

    public void write(long position, Port<FileIORequest> callback) { 
        this.position=position;
        this.callback=callback;
        channel.write(buf, position, this, this);
    }

    public AsynchronousFileChannel getChannel() {
        return channel;
    }

    public long getPosition() {
        return position;
    }
}
