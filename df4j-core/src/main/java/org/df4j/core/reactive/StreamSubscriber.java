package org.df4j.core.reactive;

import org.df4j.core.Actor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

class StreamSubscriber<T> extends Actor.StreamInput<T> implements Subscriber<T> {
    final int buffSize;
    Subscription subscription;

    public StreamSubscriber(Actor base, int bufsize) {
        base.super(bufsize);
        this.buffSize = bufsize;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscription.request(buffSize);
    }

    @Override
    protected synchronized void purge() {
        super.purge();
        int size = size();
        if (size <= buffSize/2) {
            subscription.request(buffSize - size);
        }
    }

    @Override
    public void onNext(T item) {
        if (size() == buffSize) {
            throw new IllegalStateException("input buffer overflow");
        }
        super.post(item);
    }

    @Override
    public void onError(Throwable throwable) {
        // TODO implemet
        throw new RuntimeException("onError not implemented");
    }

    @Override
    public void onComplete() {
        super.close();
    }

    public void cancel() {
        subscription.cancel();
    }
}
