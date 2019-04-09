package org.df4j.core.actor;

import org.df4j.core.asyncproc.*;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * blocks when there are no active subscribers
 */
public class StreamSubscriptionBlockingQueue<T> extends Transition.Pin
        implements SubscriptionListener<T, StreamSubscription<T>>, Publisher<T> {
    protected StreamSubscriptionQueue<T> subscriptions =  new StreamSubscriptionQueue<>(this);

    public StreamSubscriptionBlockingQueue(AsyncProc actor) {
        actor.super();
    }

    protected boolean isParameter() {
        return true;
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        subscriptions.subscribe(s);
    }

    public void onError(Throwable ex) {
        subscriptions.onError(ex);
    }

    public void onComplete() {
        subscriptions.onComplete();
    }

    @Override
    public StreamSubscription<T> current() {
        return subscriptions.current();
    }

    /**
     * when subscriber cancels subscription
     * @param subscription to be cancelled
     * @return true if no active subcriptions remain
     *         false otherwise
     */
    public synchronized boolean remove(StreamSubscription<T> subscription) {
        boolean noActive = subscriptions.remove(subscription);
        if (noActive) {
            block();
        }
        return noActive;
    }

    @Override
    public synchronized void serveRequest(StreamSubscription<T> subscription) {
        subscriptions.serveRequest(subscription);
        unblock();
    }

    @Override
    public synchronized void purge() {
        subscriptions.purge();
    }
}
