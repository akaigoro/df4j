package org.df4j.core.actor.ext;

import org.df4j.core.actor.StreamOutput;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

public abstract class StreamProcessor<M, R> extends Actor1<M> implements Publisher<R> {
	protected final StreamOutput<R> output = new StreamOutput<>(this);

    @Override
    public void subscribe(Subscriber<? super R> subscriber) {
        output.subscribe(subscriber);
    }

    @Override
    protected void runAction(M message) {
        R res = process(message);
        output.onNext(res);
    }

    @Override
    protected void completion(Throwable completionException) throws Exception {
        output.complete(completionException);
    }

    protected abstract R process(M message);

}
