package org.df4j.core.connector.messagescalar;

public class SubscriberPromise<T> extends CompletablePromise<T> implements ScalarSubscriber<T> {
    protected SimpleSubscription subscription;
    protected boolean cancelled = false;

    @Override
    public void post(T message) {
        complete(message);
    }

    @Override
    public void postFailure(Throwable ex) {
        completeExceptionally(ex);
    }

    @Override
    public synchronized void onSubscribe(SimpleSubscription subscription) {
        this.subscription = subscription;
    }

    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        if (subscription == null) {
            return cancelled;
        }
        SimpleSubscription subscription = this.subscription;
        this.subscription = null;
        cancelled = true;
        boolean result = subscription.cancel();
        return result;
    }

    public synchronized boolean isCancelled() {
        return cancelled;
    }

}
