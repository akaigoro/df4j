package org.df4j.core.connector;

import org.df4j.protocol.Completable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *  A {@link CountDownLatch} extended with asynchronous interface to publish the completion signal.
 */
public class AsyncCountDownLatch extends CountDownLatch
        implements Completable.Source,
        Completable.Publisher {
    protected Completion completionSignal = new Completion();

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
        return getCount()==0;
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

    @Override
    public boolean await(long timeoutMillis) {
        return false;
    }

    @Override
    public boolean await(long timeout, TimeUnit unit) {
        return false;
    }
}
