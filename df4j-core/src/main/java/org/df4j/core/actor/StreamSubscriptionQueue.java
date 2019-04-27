package org.df4j.core.actor;

import org.df4j.core.ScalarSubscriber;
import org.df4j.core.SubscriptionCancelledException;
import org.df4j.core.util.linked.LinkedQueue;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * non-blocking queue of {@link StreamSubscription}
 *
 * @param <T>
 */
public class StreamSubscriptionQueue<T> implements Publisher<T>, SubscriptionListener<StreamSubscription<T>> {
    protected LinkedQueue<StreamSubscription<T>> activeSubscriptions = new LinkedQueue<StreamSubscription<T>>();
    protected LinkedQueue<StreamSubscription<T>> passiveSubscriptions = new LinkedQueue<StreamSubscription<T>>();
    protected boolean completed = false;
    protected volatile Throwable completionException;

    protected void subscribe(StreamSubscription subscription) {
        subscription.onSubscribe();
        /** initiall any subscription is passive;
         * it is activated by method {@link org.reactivestreams.Subscription#request(long)}
         */
        passiveSubscriptions.offer(subscription);
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        StreamSubscription subscription = new StreamSubscription(this, s);
        subscribe(subscription);
    }

    public void subscribe(ScalarSubscriber<? super T> s) {
        StreamSubscription.Scalar2StreamSubscriber proxySubscriber = new StreamSubscription.Scalar2StreamSubscriber(s);
        StreamSubscription subscription = new StreamSubscription(this, proxySubscriber);
        subscribe(subscription);
    }

    public boolean offer(StreamSubscription<T> subscription) {
        if (subscription.isCancelled()) {
            return false;
        }
        if (subscription.isActive()) {
            activeSubscriptions.add(subscription);
            return true;
        } else {
            passiveSubscriptions.add(subscription);
            return false;
        }
    }

    @Override
    public boolean remove(StreamSubscription<T> subscription) {
        if (subscription.isActive()) {
            return activeSubscriptions.remove(subscription);
        } else {
            return passiveSubscriptions.remove(subscription);
        }
    }

    public void onComplete() {
        StreamSubscription subscription = poll();
        for (; subscription != null; subscription = poll()) {
            try {
                subscription.onComplete();
            } catch (SubscriptionCancelledException e) {
            }
        }
    }

    public void onError(Throwable ex) {
        StreamSubscription subscription = poll();
        for (; subscription != null; subscription = poll()) {
            try {
                subscription.onError(ex);
            } catch (SubscriptionCancelledException e) {
            }
        }
    }

    public void completion(Throwable completionException) {
        if (completionException == null) {
            onComplete();
        } else {
            onError(completionException);
        }
    }

    public StreamSubscription<T> peek() {
        return activeSubscriptions.peek();
    }

    public StreamSubscription poll() {
        return activeSubscriptions.poll();
    }

    public boolean noActiveSubscribers() {
        return activeSubscriptions.size() == 0;
    }
}
