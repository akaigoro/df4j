package org.df4j.nio2.net;

import org.df4j.core.connector.messagescalar.SimpleSubscription;
import org.df4j.core.connector.messagestream.StreamInput;
import org.df4j.core.connector.messagestream.StreamOutput;
import org.df4j.core.connector.messagestream.StreamPublisher;
import org.df4j.core.connector.messagestream.StreamSubscriber;
import org.df4j.core.node.Actor;
import org.df4j.core.util.Logger;

import java.nio.ByteBuffer;

public abstract class BuffProcessor extends Actor implements StreamPublisher<ByteBuffer>, StreamSubscriber<ByteBuffer> {
    protected static final Logger LOG = Logger.getLogger(BuffProcessor.class.getName());

    protected final StreamInput<ByteBuffer> input = new StreamInput<ByteBuffer>(this);
	protected final StreamOutput<ByteBuffer> output = new StreamOutput<>(this);
    protected SimpleSubscription subscription;
    {
        LOG.info(getClass().getName()+" created");
    }

    public void injectBuffers(int count, int bufLen) {
        for (int k=0; k<count; k++) {
            ByteBuffer buf=ByteBuffer.allocate(bufLen);
            input.post(buf);
        }
    }

    @Override
    public <S extends StreamSubscriber<? super ByteBuffer>> S subscribe(S subscriber) {
		output.subscribe(subscriber);
        return subscriber;
    }

    @Override
    public void onSubscribe(SimpleSubscription subscription) {
        this.subscription = subscription;
    }

    @Override
    public void post(ByteBuffer m) {
        input.post(m);
    }

    @Override
    public void postFailure(Throwable ex) {
        input.postFailure(ex);
    }

    /**
     * processes closing signal
     * @throws Exception
     */
    @Override
    public void complete() {
        input.complete();
    }

}
