package org.df4j.core;

public interface SubscriptionListener<T, S extends org.reactivestreams.Subscription> {
    void activate(S subscription);
    void cancel(S subscription);
}
