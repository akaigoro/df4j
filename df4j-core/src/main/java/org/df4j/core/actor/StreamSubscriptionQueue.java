package org.df4j.core.actor;

import org.df4j.core.ScalarSubscriber;
import org.df4j.core.util.linked.LinkedQueue;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * non-blocking queue of {@link StreamSubscription}
 *
 * @param <T>
 */
public class StreamSubscriptionQueue<T> extends LinkedQueue<StreamSubscription<T>> implements Publisher<T> {
    private final SubscriptionListener listener;
    protected boolean completed = false;
    protected volatile Throwable completionException;

    public StreamSubscriptionQueue(SubscriptionListener listener) {
        this.listener = listener;
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        StreamSubscription subscription = new StreamSubscription(listener, s);
        synchronized (this) {
            add(subscription);
        }
        s.onSubscribe(subscription);
    }

    public void subscribe(ScalarSubscriber<? super T> s) {
        Scalar2StreamSubscriber proxySubscriber = new Scalar2StreamSubscriber(s);
        StreamSubscription subscription = new StreamSubscription(listener, proxySubscriber);
        synchronized (this) {
            add(subscription);
        }
        proxySubscriber.onSubscribe(subscription);
    }

    public void complete(Throwable ex) {
        synchronized(this) {
            if (completed) {
                return;
            }
            completionException = ex;
            completed = true;
        }
        for (StreamSubscription subs = poll(); subs != null; subs = poll()) {
            subs.complete(ex);
        }
    }

    private static class Scalar2StreamSubscriber<T> implements Subscriber<T> {
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
