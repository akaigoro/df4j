package org.df4j.core.asyncproc;

public interface SubscriptionListener<T, S extends LinkedSubscription<T,S>> {
    void activate(S subscription);
    void remove(S subscription);
}
