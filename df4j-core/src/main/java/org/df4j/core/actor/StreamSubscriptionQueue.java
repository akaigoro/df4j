package org.df4j.core.actor;

import org.df4j.core.asyncproc.*;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.HashSet;

/**
 * non-blocking queue of {@link StreamSubscription}
 *
 * @param <T>
 */
public class StreamSubscriptionQueue<T> extends SubscriptionQueue<T, StreamSubscription<T>> implements Publisher<T> {
    private final SubscriptionListener listener;
 //   protected ScalarSubscriptionQueue<T> activeSubscriptions;
    protected HashSet<StreamSubscription<T>> passiveSubscriptions = new HashSet<>();
    protected boolean completed = false;
    protected volatile Throwable completionException;

    public StreamSubscriptionQueue(SubscriptionListener listener) {
        this.listener = listener;
 //       activeSubscriptions = new ScalarSubscriptionQueue<>();
    }

    protected boolean isParameter() {
        return true;
    }

    private void complete(Subscriber<? super T> s) {
        if (completionException == null) {
            s.onComplete();
        } else {
            s.onError(completionException);
        }
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        StreamSubscription subscription;
        synchronized(this) {
            if (completed) {
                subscription = null;
            } else {
                subscription = new StreamSubscription(listener, s);
                passiveSubscriptions.add(subscription);
            }
        }
        if (subscription == null) {
            complete(s);
        } else {
            s.onSubscribe(subscription);
        }
    }

    public void onError(Throwable ex) {
        HashSet<StreamSubscription<T>> passiveSubscriptions;
        synchronized(this) {
            if (completed) {
                return;
            }
            completed = true;
            passiveSubscriptions = this.passiveSubscriptions;
            this.passiveSubscriptions = null;
        }
        for (StreamSubscription<T> subs: passiveSubscriptions) {
            subs.onError(ex);
        }
        for (ScalarSubscription subs: this) {
            subs.onError(ex);
        }
    }

    public void onComplete() {
        HashSet<StreamSubscription<T>> passiveSubscriptions;
        synchronized(this) {
            if (completed) {
                return;
            }
            completed = true;
            passiveSubscriptions = this.passiveSubscriptions;
            this.passiveSubscriptions = null;
        }
        for (StreamSubscription<T> subs: passiveSubscriptions) {
            subs.onComplete();
        }
        for (ScalarSubscription subs: this) {
            ((StreamSubscription)subs).onComplete();
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
    public synchronized boolean remove(StreamSubscription<T> subscription) {
        if (subscription.getRequested() == 0) {
            passiveSubscriptions.remove(subscription);
        } else {
            super.remove(subscription);
        }
        return super.isEmpty();
    }

    @Override
    public synchronized void serveRequest(StreamSubscription<T> subscription) {
        if (completed) {
            return;
        }
        passiveSubscriptions.remove(subscription);
        super.add(subscription);
    }

    protected synchronized void purge() {
        StreamSubscription<T> current = poll();
        if (current.getRequested() == 0) {
            passiveSubscriptions.add(current);
        } else {
            super.add(current);
        }
    }
}
