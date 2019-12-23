package org.df4j.core.communicator;

import org.df4j.protocol.Completable;
import org.df4j.protocol.Signal;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *  A {@link CountDownLatch} extended asynchronous interface to publish the completion signal.
 */
public class AsyncCountDownLatch extends CountDownLatch implements Completable.Source {
    private final Lock bblock = new ReentrantLock();
    protected Completion completionSignal = new Completion();

    public AsyncCountDownLatch(int count) {
        super(count);
        if (getCount() == 0) {
            completionSignal.onComplete();
        }
    }

    public boolean isCompleted() {
        return completionSignal.isCompleted();
    }

    @Override
    public void subscribe(Completable.Observer subscriber) {
        completionSignal.subscribe(subscriber);
    }

    public void countDown() {
        if (getCount() == 0) {
            return;
        }
        bblock.lock();
        try {
            super.countDown();
            if (getCount() > 0) {
                return;
            }
        } finally {
            bblock.unlock();
        }
        completionSignal.onComplete();
    }
}
