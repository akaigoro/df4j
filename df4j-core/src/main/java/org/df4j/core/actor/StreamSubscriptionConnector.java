package org.df4j.core.actor;

import org.df4j.core.asyncproc.*;
import org.df4j.core.util.linked.LinkedQueue;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * blocks when there are no active subscribers.
 *
 * This is a angerous connector.
 * Since any subscription can be cancelled at any time,
 * it may happen that parent actor may discover that this connector is unblocked but empty,
 * and it has to recover in some way.
 *
 */
public class StreamSubscriptionConnector<T> extends StreamLock
        implements SubscriptionListener<StreamSubscription<T>>,Publisher<T>
{
    protected LinkedQueue<StreamSubscription<T>> activeSubscriptions = new LinkedQueue<StreamSubscription<T>>();
    protected LinkedQueue<StreamSubscription<T>> passiveSubscriptions = new LinkedQueue<StreamSubscription<T>>();
    protected volatile Throwable completionException;

    public StreamSubscriptionConnector(AsyncProc actor) {
        super(actor);
    }

    public StreamSubscription<T> current() {
        return activeSubscriptions.peek();
    }

    public StreamSubscription poll() {
        return activeSubscriptions.poll();
    }

    public synchronized boolean noActiveSubscribers() {
        return activeSubscriptions.size() == 0;
    }

    @Override
    public void add(StreamSubscription<T> subscription) {
        synchronized(this) {
            if (subscription.isCancelled()) {
                return;
            }
            if (!subscription.isActive()) {
                passiveSubscriptions.offer(subscription);
                return;
            }
            activeSubscriptions.offer(subscription);
        }
        unblock();
    }

    /**
     * when subscriber cancels subscription
     * @param subscription to be cancelled
     * @return true if no active subcriptions remain
     *         false otherwise
     */
    public void remove(StreamSubscription<T> subscription) {
        synchronized(this) {
            subscription.unlink();
            if (!noActiveSubscribers()) {
                return;
            }
        }
        block();
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        StreamSubscription<T> subscription = new StreamSubscription<>(this, s);
        subscription.onSubscribe();
        add(subscription);
    }

    public void onNext(T val) {
        StreamSubscription<T> subscription;
        synchronized (this) {
            System.out.println("activeSubscriptions:"+activeSubscriptions.size()+"; passiveSubscriptions:"+passiveSubscriptions.size());
            subscription = activeSubscriptions.poll();
            if (subscription == null) {
                throw new IllegalStateException();
            }
            if (subscription.requested == 0) {
                throw new IllegalStateException();
            }
            subscription.requested--;
            if (subscription.isActive()) {
                activeSubscriptions.add(subscription);
            } else {
                passiveSubscriptions.offer(subscription);
            }
        }
        subscription.onNext(val);
    }

    public void onComplete() {
        synchronized(this) {
            if (completed) {
                return;
            }
            completionException = null;
            completed = true;
        }
        for (StreamSubscription subscription = activeSubscriptions.poll(); subscription != null; subscription = activeSubscriptions.poll()) {
            subscription.complete(null);
        }
        for (StreamSubscription subscription = passiveSubscriptions.poll(); subscription != null; subscription = passiveSubscriptions.poll()) {
            subscription.complete(null);
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
        for (StreamSubscription subscription = activeSubscriptions.poll(); subscription != null; subscription = activeSubscriptions.poll()) {
            subscription.complete(ex);
        }
        for (StreamSubscription subscription = passiveSubscriptions.poll(); subscription != null; subscription = passiveSubscriptions.poll()) {
            subscription.complete(ex);
        }
    }


    public void complete(Throwable ex) {//todo
        synchronized(this) {
            if (completed) {
                return;
            }
            this.completionException = ex;
            completed = true;
        }
        for (StreamSubscription subscription = activeSubscriptions.poll(); subscription != null; subscription = activeSubscriptions.poll()) {
            subscription.complete(ex);
        }
        for (StreamSubscription subscription = passiveSubscriptions.poll(); subscription != null; subscription = passiveSubscriptions.poll()) {
            subscription.complete(ex);
        }
    }

    @Override
    public boolean moveNext() {
        return false;
    }
}
