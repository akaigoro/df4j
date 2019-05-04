package org.df4j.core.asyncproc.base;

import org.df4j.core.asyncproc.ScalarPublisher;
import org.df4j.core.asyncproc.ScalarSubscriber;
import org.df4j.core.util.SubscriptionCancelledException;
import org.df4j.core.util.linked.LinkedQueue;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * subscribers can be scalar subscribers, stream subscribers, and CompletableFutures.
 *
 * @param <T>
 */
public class ScalarSubscriptionQueue<T> extends LinkedQueue<ScalarSubscriptionImpl<T>> implements ScalarPublisher<T>, Publisher<T>  {

    public void subscribe(ScalarSubscriptionImpl<T> subscription) {
        subscription.onSubscribe();
        offer(subscription);
    }

    public void subscribe(ScalarSubscriber<? super T> s) {
        if (s == null) {
            throw new NullPointerException();
        }
        subscribe(new ScalarSubscriptionImpl(this, s));
    }

    public void subscribe(Subscriber<? super T> s) {
        ScalarSubscriptionImpl subscription = new ScalarSubscriptionImpl.Scalar2StreamSubscription(this, s);
        subscribe(subscription);
    }

    public void onComplete(T value) {
        ScalarSubscriptionImpl subscription = poll();
        for (; subscription != null; subscription = poll()) {
            try {
                subscription.onComplete(value);
            } catch (SubscriptionCancelledException e) {
            }
        }
    }

    public void onError(Throwable ex) {
        ScalarSubscriptionImpl subscription = poll();
        for (; subscription != null; subscription = poll()) {
            try {
                subscription.onError(ex);
            } catch (SubscriptionCancelledException e) {
            }
        }
    }
}