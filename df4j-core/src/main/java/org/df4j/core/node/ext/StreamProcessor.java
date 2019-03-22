package org.df4j.core.node.ext;

import org.df4j.core.connector.MulticastStreamOutput;
import org.df4j.core.node.ext.Actor1;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

public abstract class StreamProcessor<M, R> extends Actor1<M> implements Publisher<R> {
	protected final MulticastStreamOutput<R> output = new MulticastStreamOutput<>(this);

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
    protected void completion() throws Exception {
        output.onComplete();
    }

    protected abstract R process(M message);

}
