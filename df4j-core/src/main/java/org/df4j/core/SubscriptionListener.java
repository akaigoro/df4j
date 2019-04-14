package org.df4j.core;

public interface SubscriptionListener<S> {
    void activate(S subscription);
    void cancel(S subscription);
}
