package org.df4j.core.connector;

import org.df4j.protocol.Completable;

import java.util.concurrent.CountDownLatch;

/**
 *  A {@link CountDownLatch} extended with asynchronous interface to publish the completion signal.
 */
public class AsyncCountDownLatch extends CountDownLatch
        implements Completable.Source, Completable.Publisher
{
    private Completion completionSignal = new Completion();

    public AsyncCountDownLatch(int count) {
        super(count);
        if (getCount() == 0) {
            completionSignal.complete();
        }
    }

    public void countDown() {
        synchronized(this) {
            super.countDown();
            if (getCount() > 0) {
                return;
            }
        }
        completionSignal.complete();
    }

    public boolean isCompleted() {
        return completionSignal.isCompleted();
    }

    @Override
    public void subscribe(Completable.Subscriber subscriber) {
        completionSignal.subscribe(subscriber);
    }

    @Override
    public Throwable getCompletionException() {
        return null;
    }

    @Override
    public boolean isCompletedExceptionally() {
        return false;
    }
}
