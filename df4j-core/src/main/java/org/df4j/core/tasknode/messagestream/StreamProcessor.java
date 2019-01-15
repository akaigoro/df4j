package org.df4j.core.tasknode.messagestream;

import org.reactivestreams.Subscription;
import org.df4j.core.boundconnector.messagestream.StreamOutput;
import org.df4j.core.boundconnector.messagestream.StreamPublisher;
import org.df4j.core.boundconnector.messagestream.StreamSubscriber;

public abstract class StreamProcessor<M, R> extends Actor1<M> implements StreamPublisher<R> {
	protected final StreamOutput<R> output = new StreamOutput<>(this);

    public Subscription subscribe(StreamSubscriber<R> subscriber) {
        Subscription subscription = output.subscribe(subscriber);
        return subscription;
    }

    @Override
    protected void runAction(M message) {
        R res = process(message);
        output.post(res);
    }

    @Override
    protected void completion() throws Exception {
        output.complete();
    }

    protected abstract R process(M message);

}
