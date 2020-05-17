package org.df4j.core.communicator;

import org.df4j.protocol.Completable;

import java.util.concurrent.CountDownLatch;

/**
 *  A {@link CountDownLatch} extended with asynchronous interface to publish the completion signal.
 */
public class AsyncCountDownLatch extends CountDownLatch implements Completable.Source {
    protected Completion completionSignal = new Completion();

    public AsyncCountDownLatch(int count) {
        super(count);
        if (getCount() == 0) {
            completionSignal.complete();
        }
    }

    public boolean isCompleted() {
        return getCount()==0;
    }

    @Override
    public void subscribe(Completable.Observer subscriber) {
        completionSignal.subscribe(subscriber);
    }

    public void countDown() {
        if (getCount() == 0) {
            return;
        }
        synchronized(this) {
            super.countDown();
            if (getCount() > 0) {
                return;
            }
        }
        completionSignal.complete();
    }
}
