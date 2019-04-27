package org.df4j.core.actor;

public interface SubscriptionListener<S> {
    boolean offer(S subscription);
    boolean remove(S subscription);
}
