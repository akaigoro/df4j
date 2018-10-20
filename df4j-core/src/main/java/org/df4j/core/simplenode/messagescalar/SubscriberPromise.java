package org.df4j.core.simplenode.messagescalar;

import org.df4j.core.boundconnector.messagescalar.ScalarSubscriber;
import org.df4j.core.boundconnector.messagescalar.SimpleSubscription;
import org.df4j.core.tasknode.AsyncProc;

import java.util.concurrent.CancellationException;

/**
 * an unblocking single-shot output connector
 *
 * @param <T> type of future value
 */
public class SubscriberPromise<T> extends CompletablePromise<T> implements ScalarSubscriber<T> {
    protected SimpleSubscription subscription;

    public SubscriberPromise(AsyncProc task) {
        super(task);
    }

    public SubscriberPromise() {
    }

    @Override
    public void onSubscribe(SimpleSubscription subscription) {
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
