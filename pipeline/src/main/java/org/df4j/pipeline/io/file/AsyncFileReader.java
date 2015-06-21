package org.df4j.pipeline.io.file;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import org.df4j.pipeline.core.SourceNode;
import org.df4j.pipeline.df4j.core.Port;
// TODO length of reading
public  class AsyncFileReader extends SourceNode<ByteBuffer>
   implements CompletionHandler<Integer, ByteBuffer>
{
    /** here output messages return */
    protected StreamInput<ByteBuffer> myOutput=new StreamInput<ByteBuffer>();
    /** guards position variable */
    protected Semafor deltas=new Semafor();
    /** where to start reading from */
    long position=0;
    protected volatile AsynchronousFileChannel channel;
    
    public AsyncFileReader(AsynchronousFileChannel channel) {
        this.channel=channel;
        deltas.up();
    }

    public AsyncFileReader(AsynchronousFileChannel channel, long position) {
        this(channel);
        this.position=position;
    }

    @Override
    public Port<ByteBuffer> getReturnPort() {
        return myOutput;
    }

    public void injectBuffers(int count, int bufLen) {
        for (int k=0; k<count; k++) {
        	ByteBuffer buf=ByteBuffer.allocate(bufLen);
        	myOutput.post(buf);
        }
    }

    //-------------------- start reading

	@Override
    protected void act() {
        ByteBuffer buffer=myOutput.get();
        buffer.clear();
        channel.read(buffer, position, buffer, this);
    }
    
    // ------------- reading finished

    public void completed(Integer result, ByteBuffer buffer) {
        if (result==-1) {
            sinkPort.close();
        } else {
            position+=result;
            deltas.up();
            buffer.flip();
            sinkPort.post(buffer);
        }
    }

    public void failed(Throwable exc, ByteBuffer attach) {
        postFailure(exc);
    }
}