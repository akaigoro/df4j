package com.github.rfqu.df4j.io;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousByteChannel;
import java.nio.channels.AsynchronousChannel;

import com.github.rfqu.df4j.core.Port;

public class ByteIORequest extends IORequest<ByteIORequest> {
    protected AsynchronousByteChannel channel;
    
    public ByteIORequest(AsynchronousByteChannel channel) {
        this.channel = channel;
    }
    
    public ByteIORequest(AsynchronousByteChannel channel, ByteBuffer buf) {
        this.channel = channel;
        this.buf = buf;
    }
    
    public void read(ByteBuffer buf, Port<ByteIORequest> callback) { 
        this.buf=buf;
        read(callback);
    }

    public void read(Port<ByteIORequest> callback) { 
        this.callback=callback;
        channel.read(buf, this, this);
    }

    public void write(ByteBuffer buf, long position, Port<ByteIORequest> callback) { 
        this.buf = buf;
        write(callback);
    }

    public void write(Port<ByteIORequest> callback) { 
        this.callback=callback;
        channel.write(buf, this, this);
    }

    public AsynchronousChannel getChannel() {
        return channel;
    }
}
