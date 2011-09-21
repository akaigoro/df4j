package com.github.rfqu.dffw.io;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;

import com.github.rfqu.dffw.core.Port;
import com.github.rfqu.dffw.core.Link;

public class IORequest extends Link {
    protected AsynchronousFileChannel afc;
    protected ByteBuffer dst;
    protected long position;
    protected Port<IORequest> callback;
    protected Integer result=null;
    protected Throwable exc=null;
    
    public IORequest(AsynchronousFileChannel afc) {
        this.afc = afc;
    }
    
    public IORequest(AsynchronousFileChannel afc, ByteBuffer dst) {
        this.afc = afc;
        this.dst = dst;
    }
    
    public void read(ByteBuffer dst, long position, Port<IORequest> callback) { 
        this.dst=dst;
        read(position, callback);
    }

    public void read(long position, Port<IORequest> callback) { 
        this.position=position;
        this.callback=callback;
        afc.read(dst, position, this, completionHandler);
    }

    public void write(ByteBuffer dst, long position, Port<IORequest> callback) { 
        this.dst = dst;
        write(position, callback);
    }

    public void write(long position, Port<IORequest> callback) { 
        this.position=position;
        this.callback=callback;
        afc.write(dst, position, this, completionHandler);
    }

    public void clear() {
        result=null;
        exc=null;
        if (dst!=null) {
            dst.flip();
        }
    }
    
    protected static CompletionHandler<Integer, IORequest> completionHandler=new CompletionHandler<Integer, IORequest>() {

        @Override
        public void completed(Integer result, IORequest request) {
            request.result=result;
            request.callback.send(request);
        }

        @Override
        public void failed(Throwable exc, IORequest request) {
            request.exc=exc;
            request.callback.send(request);
        }
    
    };

    public AsynchronousFileChannel getAfc() {
        return afc;
    }

    public ByteBuffer getDst() {
        return dst;
    }

    public long getPosition() {
        return position;
    }

    public Port<IORequest> getCallback() {
        return callback;
    }

    public Integer getResult() {
        return result;
    }

    public Throwable getExc() {
        return exc;
    }
}
