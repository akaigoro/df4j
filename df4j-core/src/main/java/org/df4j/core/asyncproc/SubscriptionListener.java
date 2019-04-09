package org.df4j.core.asyncproc;

public interface SubscriptionListener<T, S extends ScalarSubscription<T>> {
    void serveRequest(S subscription);
    boolean remove(S subscription);
}
