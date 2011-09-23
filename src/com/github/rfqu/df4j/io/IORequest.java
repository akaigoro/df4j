package com.github.rfqu.df4j.io;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;

import com.github.rfqu.df4j.core.Link;
import com.github.rfqu.df4j.core.Port;

public class IORequest<V, R extends IORequest<V, ?>> extends Link implements CompletionHandler<V, R> {
    protected Port<R> callback;
    protected ByteBuffer buf;
    protected long position;
    protected V result=null;
    protected Throwable exc=null;
    
    public void clear() {
        result=null;
        exc=null;
        if (buf!=null) {
            buf.clear();
        }
    }

    @Override
    public void completed(V result, R request) {
        this.result=result;
        callback.send(request);
    }

    @Override
    public void failed(Throwable exc, R request) {
        this.exc=exc;
        callback.send(request);
    }
    
    public ByteBuffer getBuffer() {
        return buf;
    }

    public Port<R> getCallback() {
        return callback;
    }

    public V getResult() {
        return result;
    }

    public Throwable getExc() {
        return exc;
    }
}
