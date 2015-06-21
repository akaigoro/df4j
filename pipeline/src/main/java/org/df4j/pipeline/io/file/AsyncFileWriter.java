package org.df4j.pipeline.io.file;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.atomic.AtomicLong;
import org.df4j.pipeline.core.SinkNode;
import org.df4j.pipeline.df4j.core.DataflowVariable;

public class AsyncFileWriter extends SinkNode<ByteBuffer> implements CompletionHandler<Integer, ByteBuffer> {
    long position = 0;
    volatile AsynchronousFileChannel channel;
    ResultPoster resultPoster=new ResultPoster();
    AtomicLong bytesWritten=new AtomicLong();
    
    public AsyncFileWriter(AsynchronousFileChannel channel) {
        this.channel = channel;
    }

    public AsyncFileWriter(AsynchronousFileChannel channel, long position) {
        this(channel);
        this.position = position;
    }

    // -------------------- start writing

    protected void act(ByteBuffer buffer) {
        if (isClosed()) {
            free(buffer);
            return;
        }
        resultPoster.bufCounter.down();
        long nextPosition = position + buffer.remaining();
        channel.write(buffer, position, buffer, this);
        position = nextPosition;
    }

    @Override
    protected void complete() throws Exception {
        resultPoster.bufCounter.up();
    }

    // ------------- writing finished

    @Override
    protected void handleException(Throwable exc) {
        context.postFailure(exc);
    }

    @Override
    public void completed(Integer result, ByteBuffer buffer) {
        if (result == -1) {
            stop();
        } else if (buffer.hasRemaining()) {
            bytesWritten.getAndAdd(result.longValue());
            // write remaining bytes
            act(buffer);
            return;
        }
        bytesWritten.getAndAdd(result.longValue());
        free(buffer);
        resultPoster.bufCounter.up();
    }

    @Override
    public void failed(Throwable exc, ByteBuffer attach) {
        postFailure(exc);
    }
    
    class ResultPoster extends DataflowVariable {
        Semafor bufCounter=new Semafor();

        @Override
        protected void act() throws Exception {
            context.post(bytesWritten.get());
        }
        
    }
}