package org.df4j.core.actor;

public interface SubscriptionListener<S> {
    void activate(S subscription);
    void cancel(S subscription);
}
