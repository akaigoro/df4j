package org.df4j.core.reactive;

import org.df4j.core.Actor;
import org.df4j.core.StreamPort;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * serves single subscriber
 * demonstrates usage of class Actor.Semafor for handling back pressure
 *
 * @param <T>
 */
class OneShotPublisher<T> extends Actor.Semafor implements Publisher<T>, Subscription, StreamPort<T> {
    protected Actor base;
    protected Subscriber<? super T> subscriber;

    public OneShotPublisher(Actor base) {
        base.super();
        this.base = base;
    }

    /**
     * does nothing: counter decreases when a message is posted
     */
    @Override
    protected void purge() {
    }

    @Override
    public synchronized void subscribe(Subscriber<? super T> subscriber) {
        if (this.subscriber != null) {
            throw new IllegalStateException("subscribed already");
        }
        this.subscriber = subscriber;
        subscriber.onSubscribe(this);
    }

    @Override
    public void post(T message) {
        if (getCount()<=0) {
            throw new IllegalStateException();
        }
        aquire(1);
        subscriber.onNext(message);
    }

    @Override
    public synchronized void close() {
        super.close();
        subscriber.onComplete();
    }

    @Override
    public synchronized void request(long n) {
        super.release(n);
    }

    @Override
    public void cancel() {
        close();
    }
}
