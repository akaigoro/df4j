package org.df4j.core.core.node.messagestream;

import org.df4j.core.core.connector.messagestream.StreamOutput;
import org.df4j.core.core.connector.messagestream.StreamPublisher;
import org.df4j.core.core.connector.messagestream.StreamSubscriber;
import org.df4j.core.core.node.Actor1;

public abstract class StreamProcessor<M, R> extends Actor1<M> implements StreamPublisher<R> {
	protected final StreamOutput<R> output = new StreamOutput<>(this);

    @Override
    public <S extends StreamSubscriber<? super R>> S subscribe(S subscriber) {
		output.subscribe(subscriber);
        return subscriber;
    }

    @Override
    protected void act(M message) {
        R res = process(message);
        output.post(res);
    }

    @Override
    protected void onCompleted() {
        output.complete();
    }

    protected abstract R process(M message);

}
