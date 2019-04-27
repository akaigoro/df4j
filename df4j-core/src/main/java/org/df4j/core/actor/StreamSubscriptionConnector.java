package org.df4j.core.actor;

import org.df4j.core.SubscriptionCancelledException;
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
        implements SubscriptionListener<StreamSubscription<T>>, Publisher<T>
{
    protected StreamSubscriptionQueue<T> subscriptions = new StreamSubscriptionQueue<>();
    protected boolean completed = false;
    protected volatile Throwable completionException;

    public StreamSubscriptionConnector(AsyncProc actor) {
        super(actor);
    }

    public StreamSubscription<T> current() {
        return subscriptions.peek();
    }

    public StreamSubscription poll() {
        return subscriptions.poll();
    }

    public synchronized boolean noActiveSubscribers() {
        return subscriptions.noActiveSubscribers();
    }

    @Override
    public boolean offer(StreamSubscription<T> subscription) {
        boolean added = subscriptions.offer(subscription);
        if (added) {
            unblock();
        }
        unblock();
        return added;
    }

    /**
     * when subscriber cancels subscription
     * @param subscription to be cancelled
     * @return true if no active subcriptions remain
     *         false otherwise
     */
    public boolean remove(StreamSubscription<T> subscription) {
        boolean result;
        synchronized(this) {
            if (subscription.isLinked()) {
                subscription.unlink();
                result = true;
            } else {
                result = false;
            }
            if (!noActiveSubscribers()) {
                return result;
            }
        }
        block();
        return result;
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        StreamSubscription<T> subscription = new StreamSubscription<>(this, s);
        subscription.onSubscribe();
        offer(subscription);
    }

    /**
     * delivers the token to exactly one subscriber,
     * unless the stream is completed.
     *
     * @param token value to deliver
     * @throws SubscriptionCancelledException if all active subscriptions cancelled,
     *                and there is no one subscriber to deliver the token.
     */
    public void onNext(T token) throws SubscriptionCancelledException {
        if (token == null) {
            throw new NullPointerException();
        }
        for (;;) {
            StreamSubscription<T> subscription;
            synchronized(this) {
                if (completed) {
                    return;
                }
                subscription = subscriptions.poll();
                if (subscription == null) {
                    throw  new SubscriptionCancelledException();
                }
            }
            subscription.onNext(token);
        }
    }

    public void onComplete() {
        subscriptions.onComplete();
    }

    public void onError(Throwable ex) {
        completionException = ex;
        subscriptions.onError(ex);
    }

    public void completion(Throwable ex) {//todo
        completionException = ex;
        subscriptions.completion(ex);
    }

    @Override
    public boolean moveNext() {
        return false;
    }
}
