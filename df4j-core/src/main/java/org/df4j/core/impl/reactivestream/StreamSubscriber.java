package org.df4j.core.impl.reactivestream;

import org.df4j.core.impl.Actor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

class StreamSubscriber<T> extends Actor.StreamInput<T> implements Subscriber<T> {
    protected final int buffSize;
    protected int freeCount;
    protected Subscription subscription;

    public StreamSubscriber(Actor base, int bufsize) {
        base.super(bufsize);
        this.buffSize = bufsize;
        this.freeCount = bufsize;
    }

    @Override
    public synchronized void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscription.request(buffSize);
        freeCount -= buffSize;
    }

    @Override
    protected synchronized void purge() {
        super.purge();
        if (closeRequested) {
            return;
        }
        freeCount++;
        if (freeCount >= buffSize/2) {
            subscription.request(freeCount);
            freeCount = 0;
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
        super.postFailure(throwable);
    }

    @Override
    public void onComplete() {
        super.close();
    }

    public void cancel() {
        subscription.cancel();
    }
}
