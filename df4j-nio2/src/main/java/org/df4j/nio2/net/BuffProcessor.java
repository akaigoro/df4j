package org.df4j.nio2.net;

import org.df4j.core.connector.messagescalar.SimpleSubscription;
import org.df4j.core.connector.messagestream.StreamInput;
import org.df4j.core.connector.messagestream.StreamOutput;
import org.df4j.core.connector.messagestream.StreamPublisher;
import org.df4j.core.connector.messagestream.StreamSubscriber;
import org.df4j.core.node.Action;
import org.df4j.core.node.AsyncTask;
import org.df4j.core.util.Logger;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;

public abstract class BuffProcessor extends AsyncTask {
    protected static final Logger LOG = Logger.getLogger(BuffProcessor.class.getName());

    private AsyncSocketChannel asyncSocketChannel;
    public final StreamInput<ByteBuffer> input = new StreamInput<ByteBuffer>(this);
    public final StreamOutput<ByteBuffer> output = new StreamOutput<>(this);

    {
        LOG.info(getClass().getName()+" created");
    }

    public BuffProcessor(AsyncSocketChannel asyncSocketChannel) {
        this.asyncSocketChannel = asyncSocketChannel;
    }

    public void injectBuffers(int count, int bufLen) {
        for (int k=0; k<count; k++) {
            ByteBuffer buf=ByteBuffer.allocate(bufLen);
            input.post(buf);
        }
    }

    long timeout=0;

    //-------------------- start reading

    @Action
    protected void act(ByteBuffer buffer) {
        if (input.isClosed()) {
            output.close();
            output.postFailure(new AsynchronousCloseException());
            LOG.info("conn "+ asyncSocketChannel.name+": input.isClosed()");
            return;
        }
        buffer.clear();
        doIO(buffer);
    }

    // ------------- reading finished
    public void completed(Integer result, ByteBuffer buffer) {
        LOG.info("conn "+ asyncSocketChannel.name+": read() completed "+result);
        if (result==-1) {
            output.complete();
            asyncSocketChannel.close();
        } else {
            buffer.flip();
            output.post(buffer);
            // start next reading only after this reading is finished,
            // to keep buffer ordering
            this.start();
        }
    }

    public void failed(Throwable exc, ByteBuffer attach) {
        LOG.info("conn "+ asyncSocketChannel.name+": read() failed "+exc);
        if (exc instanceof AsynchronousCloseException) {
            asyncSocketChannel.close();
        } else {
            this.start(); // let subsequent requests fail
            output.postFailure(exc);
        }
    }

    protected abstract void doIO(ByteBuffer buffer);

}
