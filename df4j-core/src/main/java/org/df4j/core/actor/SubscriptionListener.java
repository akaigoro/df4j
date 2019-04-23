package org.df4j.core.actor;

public interface SubscriptionListener<S> {
    void add(S subscription);
    void remove(S subscription);
}
