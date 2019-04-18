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

    @Override
    public void activate(StreamSubscription<T> subscription) {
        activeSubscriptions.offer(subscription);
    }

    @Override
    public synchronized void remove(StreamSubscription<T> subscription) {
        subscription.unlink();
    }

    protected void subscribe(StreamSubscription subscription) {
        subscription.onSubscribe();
        add(subscription);
    }

    protected void add(StreamSubscription subscription) {
        synchronized (this) {
            if (subscription.isCancelled()) {
                return;
            }
            if (subscription.isActive()) {
                activate(subscription);
            } else {
                passiveSubscriptions.offer(subscription);
            }
        }
    }

    protected StreamSubscription<T> poll() {
        return activeSubscriptions.poll();
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

    public void complete(Throwable ex) {
        synchronized(this) {
            if (completed) {
                return;
            }
            completionException = ex;
            completed = true;
        }
        for (StreamSubscription subscription = activeSubscriptions.poll(); subscription != null; subscription = activeSubscriptions.poll()) {
            subscription.complete(ex);
        }
        for (StreamSubscription subscription = passiveSubscriptions.poll(); subscription != null; subscription = passiveSubscriptions.poll()) {
            subscription.complete(ex);
        }
    }

}
