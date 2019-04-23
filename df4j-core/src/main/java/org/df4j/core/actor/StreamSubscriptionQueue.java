package org.df4j.core.actor;

import org.df4j.core.ScalarSubscriber;
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
        add(subscription);
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

    public synchronized void add(StreamSubscription subscription) {
        if (subscription.isCancelled()) {
            return;
        }
        if (subscription.isActive()) {
            activeSubscriptions.add(subscription);
        } else {
            passiveSubscriptions.offer(subscription);
        }
    }

    @Override
    public synchronized void remove(StreamSubscription<T> subscription) {
        if (subscription.isCancelled()) {
            return;
        }
        if (subscription.isActive()) {
            activeSubscriptions.remove(subscription);
        } else {
            passiveSubscriptions.remove(subscription);
        }
    }

}
