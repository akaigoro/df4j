package org.df4j.protocol;

/**
 * for on-shot subscriptions, where subscriber is unsubscribed by publisher after single message transmission.
 */
public interface SimpleSubscription extends org.reactivestreams.Subscription {
    @Override
    default void request(long n) {
        throw new UnsupportedOperationException();
    }
}
