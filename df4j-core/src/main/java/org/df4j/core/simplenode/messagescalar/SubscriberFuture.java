package org.df4j.core.simplenode.messagescalar;

import org.df4j.core.boundconnector.messagescalar.ScalarSubscriber;
import org.df4j.core.boundconnector.messagescalar.SimpleSubscription;

import java.util.concurrent.*;

/**
 * @param <T> the type of future value
 */
public class SubscriberFuture<T> extends CompletableFuture<T> implements ScalarSubscriber<T> {
    protected SimpleSubscription subscription;

    @Override
    public synchronized void onSubscribe(SimpleSubscription subscription) {
        if (isCancelled()) {
            throw new IllegalStateException("cancelled already");
        }
        if (isDone()) {
            throw new IllegalStateException("completed already");
        }
        this.subscription = subscription;
    }

    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        boolean result = super.cancel(mayInterruptIfRunning);
        if (subscription != null) {
            result = subscription.cancel();
            this.subscription = null;
        }
        return result;
    }

}
