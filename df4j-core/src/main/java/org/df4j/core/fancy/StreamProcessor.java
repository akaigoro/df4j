package org.df4j.core.fancy;

import org.df4j.core.port.OutMessage;

import java.util.concurrent.Flow;

public abstract class StreamProcessor<M, R> extends Hactor<M> implements Flow.Processor<M, R> {
	protected final OutMessage<R> output = new OutMessage<>(this);

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
    protected void completion(Throwable completionException) {
        output.completion(completionException);
    }

    protected abstract R process(M message);

}
