package org.df4j.core.asyncproc;

public interface SubscriptionListener<T> {
    void serveRequest(ScalarSubscription<T> subscription);
    boolean remove(ScalarSubscription<T> subscription);
}
