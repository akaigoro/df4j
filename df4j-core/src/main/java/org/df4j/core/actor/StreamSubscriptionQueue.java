package org.df4j.core.actor;

import org.df4j.core.asyncproc.*;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.HashSet;
import java.util.Iterator;

/**
 * non-blocking queue of {@link StreamSubscription}
 *
 * @param <T>
 */
public class StreamSubscriptionQueue<T> extends SubscriptionQueue<T, StreamSubscription<T>> implements Publisher<T> {
    private final SubscriptionListener listener;
    protected HashSet<StreamSubscription<T>> passiveSubscriptions = new HashSet<>();
    protected boolean completed = false;
    protected volatile Throwable completionException;

    public StreamSubscriptionQueue(SubscriptionListener listener) {
        this.listener = listener;
    }

    public boolean noActiveSubscribers() {
        return isEmpty();
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        StreamSubscription subscription;
        synchronized (this) {
            subscription = new StreamSubscription(listener, s);
            passiveSubscriptions.add(subscription);
        }
        s.onSubscribe(subscription);
        subscription.setInitialized();
    }

    public void onError(Throwable ex) {
        synchronized(this) {
            if (completed) {
                return;
            }
            completionException = ex;
            completed = true;
        }
        for (Iterator<StreamSubscription<T>> it = passiveSubscriptions.iterator(); it.hasNext();) {
            StreamSubscription<T> subs = it.next();
            it.remove();
            subs.onError(ex);
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
        for (Iterator<StreamSubscription<T>> it = passiveSubscriptions.iterator(); it.hasNext();) {
            StreamSubscription<T> subs = it.next();
            it.remove();
            subs.onComplete();
        }
        for (StreamSubscription subs = poll(); subs != null; subs = poll()) {
            subs.onComplete();
        }
    }

    public synchronized StreamSubscription<T> current() {
        return super.peek();
    }

    /**
     * when subscriber cancels subscription
     * @param subscription to be cancelled
     * @return true if no active suscriptions remain
     *         false otherwise
     */
    @Override
    public synchronized void remove(StreamSubscription<T> subscription) {
        if (subscription.getRequested() == 0) {
            passiveSubscriptions.remove(subscription);
        } else {
            super.remove(subscription);
        }
    }

    @Override
    public synchronized void activate(StreamSubscription<T> subscription) {
        if (completed) {
            return;
        }
        passiveSubscriptions.remove(subscription);
        super.add(subscription);
    }

    protected synchronized void purge() {
        StreamSubscription<T> current = poll();
        if (current == null) {
            return;
        }
        if (current.getRequested() == 0) {
            passiveSubscriptions.add(current);
        } else {
            super.add(current);
        }
    }
}
