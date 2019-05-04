package org.df4j.core.asyncproc.base;

import io.reactivex.disposables.Disposable;
import org.df4j.core.asyncproc.ScalarSubscriber;
import org.df4j.core.util.SubscriptionCancelledException;
import org.df4j.core.util.linked.Link;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class ScalarSubscriptionImpl<T> extends Link<ScalarSubscriptionImpl<T>> implements Disposable {
    private final ScalarSubscriptionQueue<T> parent;
    protected ScalarSubscriber subscriber;
    private volatile boolean inOnSubscribe = false;

    public ScalarSubscriptionImpl(ScalarSubscriptionQueue<T> parent, ScalarSubscriber subscriber) {
        if (parent == null || subscriber == null) {
            throw new NullPointerException();
        }
        this.subscriber = subscriber;
        this.parent = parent;
    }

    public synchronized boolean isCancelled() {
        return subscriber == null;
    }

    public void onSubscribe() {
        inOnSubscribe = true;
        subscriber.onSubscribe(this);
        inOnSubscribe = false;
    }

    protected void unlink() {
        super.unlink();
    }

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
        parent.remove(this);
    }

    public synchronized ScalarSubscriber extractScalarSubscriber() throws SubscriptionCancelledException {
        if (isCancelled()) {
            throw new SubscriptionCancelledException();
        } else {
            ScalarSubscriber subscriberLoc = subscriber;
            subscriber = null;
            return subscriberLoc;
        }
    }

    public <T> void onComplete(T value) throws SubscriptionCancelledException {
        extractScalarSubscriber().onComplete(value);
    }

    public void onError(Throwable t) throws SubscriptionCancelledException {
        extractScalarSubscriber().onError(t);
    }

    @Override
    public void dispose() {
        cancel();
    }

    @Override
    public boolean isDisposed() {
        return isCancelled();
    }

    public static class Scalar2StreamSubscription<T> extends ScalarSubscriptionImpl<T> implements Subscription {
        private Stream2ScalarSubscriber scalarSubscriber;

        public Scalar2StreamSubscription(ScalarSubscriptionQueue<T> parent, Subscriber<T> streamSubscriber) {
            super(parent, new Stream2ScalarSubscriber<T>(streamSubscriber));
            scalarSubscriber = (Stream2ScalarSubscriber) super.subscriber;
        }

        @Override
        public void request(long n) {
            if (n <= 0) {
                scalarSubscriber.onError(new IllegalArgumentException());
                return;
            }
            if (isCancelled()) {
                return;
            }
            scalarSubscriber.request();
        }
    }

}
