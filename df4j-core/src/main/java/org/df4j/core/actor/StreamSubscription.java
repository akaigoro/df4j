package org.df4j.core.actor;

import org.df4j.core.ScalarSubscriber;
import org.df4j.core.util.linked.Link;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.CancellationException;

/**
 *
 * @param <T>
 */
public class StreamSubscription<T> extends Link<StreamSubscription<T>> implements Subscription {
    private long requested = 0;
    protected SubscriptionListener<StreamSubscription<T>> listener;
    private Subscriber subscriber;
    private volatile boolean inOnSubscribe = false;

    public StreamSubscription(SubscriptionListener<StreamSubscription<T>> listener, Subscriber subscriber) {
        this.listener = listener;
        this.subscriber = subscriber;
    }

    public synchronized boolean isCancelled() {
        return subscriber == null;
    }

    public boolean isActive() {
        return requested > 0;
    }

    protected void unlink() {
        super.unlink();
    }

    protected void onSubscribe() {
        inOnSubscribe = true;
        subscriber.onSubscribe(this);
        inOnSubscribe = false;
    }

    @Override
    public synchronized void request(long n) {
        if (n <= 0) {
            onError(new IllegalArgumentException());
            return;
        }
        boolean wasPassive;
        synchronized (this) {
            if (isCancelled()) {
                return;
            }
            wasPassive = requested == 0;
            requested += n;
            if (requested < 0) { // overflow
                requested = Long.MAX_VALUE;
            }
            if (inOnSubscribe) {
                return;
            }
        }
        if (wasPassive) {
            unlink();
            listener.activate(this);
        }
    }

    public void onNext(T value) {
        Subscriber subscriberLoc;
        synchronized (this) {
            if (isCancelled()) {
                throw new CancellationException();
            }
            if (requested == 0) {
                throw new IllegalStateException();
            }
            requested--;
            subscriberLoc = subscriber;
        }
        subscriberLoc.onNext(value);
    }

    @Override
    public void cancel() {
        synchronized(this){
            if (isCancelled()) {
                return;
            }
            subscriber = null;
            if (inOnSubscribe) {
                return;
            }
        }
        listener.remove((StreamSubscription<T>) this);
    }

    protected Subscriber extractSubscriber() {
        synchronized (this) {
      //      waitInitialized();
            if (isCancelled()) {
                return null;
            } else {
                Subscriber subscriberLoc = subscriber;
                subscriber = null;
                return subscriberLoc;
            }
        }
    }

    protected void complete(Throwable ex) {
        Subscriber subscriberLoc;
        synchronized (this) {
            if (isCancelled()) {
                return;
            }
            subscriberLoc = extractSubscriber();
            if (subscriberLoc == null) {
                return;
            }
        }
        if (ex == null) {
            subscriberLoc.onComplete();
        } else {
            subscriberLoc.onError(ex);
        }
    }

    public void onComplete() {
        complete(null);
    }

    public void onError(Throwable ex) {
        complete(ex);
    }

    static class Scalar2StreamSubscriber<T> implements Subscriber<T> {
        private ScalarSubscriber scalarSubscriber;
        private Subscription subscription;

        public Scalar2StreamSubscriber(ScalarSubscriber<? super T> s) {
            scalarSubscriber = s;
        }

        @Override
        public void onSubscribe(Subscription s) {
            subscription = s;
            s.request(1);
        }

        @Override
        public void onNext(T t) {
            scalarSubscriber.onComplete(t);
            subscription.cancel();
        }

        @Override
        public void onError(Throwable t) {
            scalarSubscriber.onError(t);
            subscription.cancel();
        }

        @Override
        public void onComplete() {
            scalarSubscriber.onComplete(null);
            subscription.cancel();
        }
    }
}
