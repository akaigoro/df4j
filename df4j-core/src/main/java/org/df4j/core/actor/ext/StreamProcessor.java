package org.df4j.core.actor.ext;

import org.df4j.core.actor.StreamOutput;
import org.df4j.core.protocols.Flow;

public abstract class StreamProcessor<M, R> extends Hactor<M> implements Flow.Publisher<R> {
	protected final StreamOutput<R> output = new StreamOutput<>(this);

    @Override
    public void subscribe(Flow.Subscriber<? super R> subscriber) {
        output.subscribe(subscriber);
    }

    @Override
    protected void runAction(M message) {
        R res = process(message);
        output.onNext(res);
    }

    @Override
    protected void completion(Throwable completionException) throws Exception {
        output.completion(completionException);
    }

    protected abstract R process(M message);

}
