package org.df4j.core.actor;

import org.df4j.core.SubscriptionListener;
import org.df4j.core.util.linked.LinkedQueue;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * non-blocking queue of {@link StreamSubscription}
 *
 * @param <T>
 */
public class StreamSubscriptionQueue<T> extends LinkedQueue<StreamSubscription<T>> implements Publisher<T> {
    private final SubscriptionListener listener;
    protected boolean completed = false;
    protected volatile Throwable completionException;
    private int activeNumber = 0;

    public StreamSubscriptionQueue(SubscriptionListener listener) {
        this.listener = listener;
    }

    public synchronized boolean noActiveSubscribers() {
        return activeNumber == 0;
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        StreamSubscription subscription = new StreamSubscription(listener, s);
        synchronized (this) {
            add(subscription);
        }
        s.onSubscribe(subscription);
        subscription.setInitialized();
    }

    public synchronized void activate(StreamSubscription<T> simpleSubscription) {
        activeNumber++;
    }

    @Override
    public synchronized StreamSubscription<T> poll() {
        for (;;) {
            StreamSubscription<T> subscription = super.poll();
            if (subscription == null) {
                return null;
            } else if (subscription.isCancelled()) {
                continue;
            } else if (subscription.requested > 0) {
                activeNumber--;
            }
            return subscription;
        }
    }

    public void onError(Throwable ex) {
        synchronized(this) {
            if (completed) {
                return;
            }
            completionException = ex;
            completed = true;
        }
        for (StreamSubscription subs = poll(); subs != null; subs = poll()) {
            subs.onError(ex);
        }
    }

    public void onComplete() {
        synchronized(this) {
            if (completed) {
                return;
            }
            completed = true;
        }
        for (StreamSubscription subs = poll(); subs != null; subs = poll()) {
            subs.onComplete();
        }
    }

    public synchronized StreamSubscription<T> current() {
        return super.peek();
    }

    protected synchronized StreamSubscription<T> next() {
        return poll();
    }
}
