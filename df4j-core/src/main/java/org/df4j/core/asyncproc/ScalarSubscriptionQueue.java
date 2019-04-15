package org.df4j.core.asyncproc;

import org.df4j.core.ScalarPublisher;
import org.df4j.core.ScalarSubscriber;
import org.df4j.core.util.linked.LinkedQueue;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.CompletableFuture;

public class ScalarSubscriptionQueue<T> extends LinkedQueue<ScalarSubscription<T>> implements ScalarPublisher<T> {

    public void subscribe(ScalarSubscriber<? super T> s) {
        ScalarSubscription subscription = new ScalarSubscription(this, s);
        synchronized (this) {
            add(subscription);
        }
        s.onSubscribe(subscription);
    }

    public void subscribe(Subscriber<? super T> s) {
        Scalar2StreamSubscription subscription = new Scalar2StreamSubscription(this, s);
        synchronized (this) {
            add(subscription);
        }
        s.onSubscribe(subscription);
    }

    public void subscribe(CompletableFuture<? super T> cf) {
        ScalarSubscriber<T> proxySubscriber = new CompletableFuture2ScalarSubscriber<>(cf);
        ScalarSubscription subscription = new ScalarSubscription(this, proxySubscriber);
        synchronized (this) {
            add(subscription);
        }
        proxySubscriber.onSubscribe(subscription);
    }

    public void onComplete(T value) {
        ScalarSubscription subscription = poll();
        for (; subscription != null; subscription = poll()) {
            subscription.onComplete(value);
        }
    }

    public void onError(Throwable ex) {
        ScalarSubscription subscription = poll();
        for (; subscription != null; subscription = poll()) {
            subscription.onError(ex);
        }
    }

    static class Scalar2StreamSubscription<T> extends ScalarSubscription<T> implements Subscription {
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

    static class Stream2ScalarSubscriber<T> implements ScalarSubscriber<T> {
        private final Subscriber<T> streamSubscriber;
        private boolean requested = false;
        private boolean completed = false;
        private T completionValue;
        private Throwable completionThrowable;

        Stream2ScalarSubscriber(Subscriber<T> streamSubscriber) {
            this.streamSubscriber = streamSubscriber;
        }

        public void request() {
            requested = true;
            if (!completed) {
                return;
            }
            if (completionThrowable == null) {
                streamSubscriber.onNext(completionValue);
                streamSubscriber.onComplete();
            } else {
                streamSubscriber.onError(completionThrowable);
            }
        }

        @Override
        public void onSubscribe(ScalarSubscription subscription) {
            streamSubscriber.onSubscribe((Subscription) subscription);
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
            if (requested) {
                streamSubscriber.onError(t);
            } else {
                completionThrowable = t;
                completed = true;
            }
        }
    }

    static class CompletableFuture2ScalarSubscriber<T> implements ScalarSubscriber<T> {
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