package org.df4j.core.asyncproc;

import org.df4j.core.util.SubscriptionCancelledException;
import org.df4j.core.util.linked.LinkedQueue;
import org.reactivestreams.Subscriber;

import java.util.concurrent.CompletableFuture;

/**
 * subscribers can be scalar subscribers, stream subscribers, and CompletableFutures.
 *
 * @param <T>
 */
public class ScalarSubscriptionQueue<T> extends LinkedQueue<ScalarSubscription<T>> implements ScalarPublisher<T> {

    public void subscribe(ScalarSubscription<T> subscription) {
        subscription.onSubscribe();
        offer(subscription);
    }

    public void subscribe(ScalarSubscriber<? super T> s) {
        if (s == null) {
            throw new NullPointerException();
        }
        subscribe(new ScalarSubscription(this, s));
    }

    public void subscribe(CompletableFuture<? super T> cf) {
        if (cf == null) {
            throw new NullPointerException();
        }
        ScalarSubscriber<T> proxySubscriber = new ScalarSubscription.CompletableFuture2ScalarSubscriber<>(cf);
        subscribe(proxySubscriber);
    }

    public void subscribe(Subscriber<? super T> s) {
        ScalarSubscription subscription = new ScalarSubscription.Scalar2StreamSubscription(this, s);
        subscribe(subscription);
    }

    public void onComplete(T value) {
        ScalarSubscription subscription = poll();
        for (; subscription != null; subscription = poll()) {
            try {
                subscription.onComplete(value);
            } catch (SubscriptionCancelledException e) {
            }
        }
    }

    public void onError(Throwable ex) {
        ScalarSubscription subscription = poll();
        for (; subscription != null; subscription = poll()) {
            try {
                subscription.onError(ex);
            } catch (SubscriptionCancelledException e) {
            }
        }
    }
}