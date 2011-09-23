package com.github.rfqu.df4j.io;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousByteChannel;
import java.nio.channels.AsynchronousChannel;

import com.github.rfqu.df4j.core.Port;

public class ByteIORequest extends IORequest<Integer, ByteIORequest> {
    protected AsynchronousByteChannel channel;
    
    public ByteIORequest(AsynchronousByteChannel channel) {
        this.channel = channel;
    }
    
    public ByteIORequest(AsynchronousByteChannel channel, ByteBuffer buf) {
        this.channel = channel;
        super.buf = buf;
    }
    
    public void read(ByteBuffer buf, Port<ByteIORequest> callback) { 
        super.buf=buf;
        read(callback);
    }

    public void read(Port<ByteIORequest> callback) { 
        super.callback=callback;
        channel.read(buf, this, this);
    }

    public void write(ByteBuffer buf, Port<ByteIORequest> callback) { 
        super.buf = buf;
        write(callback);
    }

    public void write(Port<ByteIORequest> callback) { 
        super.callback=callback;
        channel.write(buf, this, this);
    }

    public AsynchronousChannel getChannel() {
        return channel;
    }
}
