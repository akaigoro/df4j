package org.df4j.core.tasknode.messagestream;

import org.df4j.core.boundconnector.messagestream.StreamOutput;
import org.df4j.core.boundconnector.messagestream.StreamPublisher;
import org.df4j.core.boundconnector.messagestream.StreamSubscriber;
import org.df4j.core.tasknode.Action;

public abstract class StreamProcessor<M, R> extends Actor1<M> implements StreamPublisher<R> {
	protected final StreamOutput<R> output = new StreamOutput<>(this);

    @Override
    public <S extends StreamSubscriber<? super R>> S subscribe(S subscriber) {
		output.subscribe(subscriber);
        return subscriber;
    }

    @Action
    protected void act(M message) {
        if (message == null) {
            output.complete();
        } else {
            R res = process(message);
            output.post(res);
        }
    }

    protected abstract R process(M message);

}
