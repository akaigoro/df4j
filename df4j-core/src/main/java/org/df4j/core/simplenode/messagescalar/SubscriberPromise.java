package org.df4j.core.simplenode.messagescalar;

import org.df4j.core.boundconnector.messagescalar.ScalarSubscriber;
import org.df4j.core.boundconnector.messagescalar.SimpleSubscription;
import org.df4j.core.tasknode.AsyncProc;

import java.util.concurrent.CancellationException;

/**
 * an unblocking single-shot output connector
 *
 * @param <T>
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

    /**
     * wrong API design. Future is not a task.
     * @param mayInterruptIfRunning
     * @return
     */
    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        if (subscription != null) {
            subscription.cancel();
        }
        return super.cancel(mayInterruptIfRunning);
   }
}
