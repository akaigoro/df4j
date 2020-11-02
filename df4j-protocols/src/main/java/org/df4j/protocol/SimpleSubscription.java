package org.df4j.protocol;

import org.reactivestreams.Publisher;

/**
 * for on-shot subscriptions, where subscriber is unsubscribed by publisher after single message transmission.
 */
public interface SimpleSubscription {
    /**
     *  Dispose the resource, the operation should be idempotent.
     */
    void cancel();

    /**
     * @return true if this resource has been disposed.
     */
    boolean isCancelled();
}
