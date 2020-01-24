package org.df4j.protocol;

/**
 * for on-shot subscriptions, where subscriber is unsubscribed by publisher after single message transmission.
 */
public interface SimpleSubscription {
    /**
     *  Dispose the resource, the operation should be idempotent.
     */
    void cancel();

    /**
     * Request to stop sending data and clean up resources.
     * <p>
     * Data may still be sent to meet previously signalled demand after calling cancel.
     * @return true if this resource has been disposed.
     */
    boolean isCancelled();
}
