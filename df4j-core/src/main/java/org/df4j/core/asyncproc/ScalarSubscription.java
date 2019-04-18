package org.df4j.core.asyncproc;

import org.df4j.core.ScalarSubscriber;
import org.df4j.core.util.linked.Link;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.CompletableFuture;

public class ScalarSubscription<T> extends Link<ScalarSubscription<T>> {
    private final ScalarSubscriptionQueue<T> parent;
    protected ScalarSubscriber subscriber;
    private volatile boolean inOnSubscribe = false;

    public ScalarSubscription(ScalarSubscriptionQueue<T> parent, ScalarSubscriber subscriber) {
        if (parent == null || subscriber == null) {
            throw new NullPointerException();
        }
        this.subscriber = subscriber;
        this.parent = parent;
    }

    public synchronized boolean isCancelled() {
        return subscriber == null;
    }

    protected void onSubscribe() {
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

    protected synchronized ScalarSubscriber extractScalarSubscriber() {
        if (isCancelled()) {
            return null;
        } else {
            ScalarSubscriber subscriberLoc = subscriber;
            subscriber = null;
            return subscriberLoc;
        }
    }

    public <T> void onComplete(T value) {
        ScalarSubscriber subscriberLoc = extractScalarSubscriber();
        if (subscriberLoc == null) {
            return;
        }
        subscriberLoc.onComplete(value);
    }

    public void onError(Throwable t) {
        ScalarSubscriber subscriberLoc = extractScalarSubscriber();
        if (subscriberLoc == null) {
            return;
        }
        subscriberLoc.onError(t);
    }

    protected static class Scalar2StreamSubscription<T> extends ScalarSubscription<T> implements Subscription {
        private Stream2ScalarSubscriber scalarSubscriber;

        public Scalar2StreamSubscription(ScalarSubscriptionQueue<T> parent, Subscriber<T> streamSubscriber) {
            super(parent, new Stream2ScalarSubscriber<T>(streamSubscriber));
            scalarSubscriber = (Stream2ScalarSubscriber) super.subscriber;
        }

        @Override
        public void request(long n) {
            if (n <= 0) {
                onError(new IllegalArgumentException());
                return;
            }
            if (isCancelled()) {
                return;
            }
            scalarSubscriber.request();
        }
    }

    protected static class Stream2ScalarSubscriber<T> implements ScalarSubscriber<T> {
        private final Subscriber<? super T> streamSubscriber;
        private boolean requested = false;
        private boolean completed = false;
        private T completionValue;

        Stream2ScalarSubscriber(Subscriber<? super T> streamSubscriber) {
            this.streamSubscriber = streamSubscriber;
        }

        public void request() {
            requested = true;
            if (!completed) {
                return;
            }
            streamSubscriber.onNext(completionValue);
            streamSubscriber.onComplete();
        }

        @Override
        public void onSubscribe(ScalarSubscription subscription) {
            streamSubscriber.onSubscribe((Scalar2StreamSubscription) subscription);
        }

        @Override
        public void onComplete(T t) {
            if (requested) {
                streamSubscriber.onNext(t);
                streamSubscriber.onComplete();
            } else {
                completionValue = t;
                completed = true;
            }
        }

        @Override
        public void onError(Throwable t) {
            streamSubscriber.onError(t);
        }
    }

    protected static class CompletableFuture2ScalarSubscriber<T> implements ScalarSubscriber<T> {
        private final CompletableFuture<? super T> cf;

        public CompletableFuture2ScalarSubscriber(CompletableFuture<? super T> cf) {
            this.cf = cf;
        }

        @Override
        public void onSubscribe(ScalarSubscription s) {}

        @Override
        public void onComplete(T t) {
            cf.complete(t);
        }

        @Override
        public void onError(Throwable t) {
            cf.completeExceptionally(t);
        }
    }
}
