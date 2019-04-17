package org.df4j.core.actor;

import org.df4j.core.asyncproc.*;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * blocks when there are no active subscribers
 */
public class StreamSubscriptionBlockingQueue<T> extends Transition.Param<StreamSubscription<T>>
        implements SubscriptionListener<StreamSubscription<T>>, Publisher<T>
{
    protected StreamSubscriptionQueue<T> subscriptions =  new StreamSubscriptionQueue<>(this);
    /** number of active subscripions in the queue
     * current is always active or null
     */
    private int activeNumber = 0;

    public StreamSubscriptionBlockingQueue(AsyncProc actor) {
        actor.super();
    }

    public synchronized boolean noActiveSubscribers() {
        return activeNumber == 0;
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        StreamSubscription subscription = new StreamSubscription(this, s);
        synchronized (this) {
            if (current == null) {
                current = subscription;
            } else {
                subscriptions.add(subscription);
            }
        }
        s.onSubscribe(subscription);
    }

    protected void complete(Throwable ex) {
        if (current != null) {
            current.complete(ex);
            current = null;
        }
        subscriptions.complete(ex);
        super.complete();
    }

    public void onComplete() {
        complete(null);
    }

    public void onError(Throwable ex) {
        complete(ex);
    }

    /**
     * when subscriber cancels subscription
     * @param subscription to be cancelled
     * @return true if no active subcriptions remain
     *         false otherwise
     */
    public synchronized void cancel(StreamSubscription<T> subscription) {
        subscriptions.remove(subscription);
        if (noActiveSubscribers()) {
            block();
        }
    }

    @Override
    public synchronized void activate(StreamSubscription<T> subscription) {
        activeNumber++;
        unblock();
    }

    @Override
    public synchronized boolean moveNext() {
        boolean noMoreActive;
        synchronized (this) {
            if (current != null) {
                if (!current.isCancelled()) {
                    subscriptions.add(current);
                    if (current.isActive()) {
                        activeNumber++;
                    }
                }
                current = null;
            }
            if (activeNumber == 0) {
                noMoreActive = true;
            } else {
                for (;;) {
                    // skip non-ready subscriptions
                    current = subscriptions.poll();
                    if (current == null) {
                        noMoreActive = true;
                        break;
                    } else if (current.isCancelled()) {
                        continue; // throw away cancelled
                    } else if (current.isActive()) {
                        activeNumber--;
                        noMoreActive = false;
                        break;
                    } else {
                        subscriptions.add(current);
                    }
                }
            }
        }
        if (noMoreActive) {
            block();
        }
        return !noMoreActive;
    }
}
