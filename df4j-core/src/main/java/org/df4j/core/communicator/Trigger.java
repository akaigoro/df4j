package org.df4j.core.communicator;

import org.df4j.protocol.Signal;

import java.util.LinkedList;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link Trigger} can be considered as a one-shot AsyncSemaphore: once released, it always satisfies aquire()
 */
public class Trigger implements Signal.Publisher, Signal.Subscriber {
    private final Lock bblock = new ReentrantLock();
    private final Condition completedCond = bblock.newCondition();
    protected LinkedList<Signal.Subscriber> subscribers;
    protected boolean completed;

    public boolean isCompleted() {
        bblock.lock();
        try {
            return completed;
        } finally {
            bblock.unlock();
        }
    }

    protected LinkedList<Signal.Subscriber> getSubscribers() {
        bblock.lock();
        try {
            if (subscribers == null) {
                subscribers = new LinkedList<>();
            }
            return subscribers;
        } finally {
            bblock.unlock();
        }
    }

    public void subscribe(Signal.Subscriber subscriber) {
        bblock.lock();
        try {
            if (!completed) {
                getSubscribers().add(subscriber);
                return;
            }
        } finally {
            bblock.unlock();
        }
        subscriber.onComplete();
    }

    public boolean unsubscribe(Signal.Subscriber co) {
        bblock.lock();
        try {
            return subscribers.remove(co);
        } finally {
            bblock.unlock();
        }
    }

    public void onComplete() {
        LinkedList<Signal.Subscriber> subs;
        bblock.lock();
        try {
            if (completed) {
                return;
            }
            completed = true;
            completedCond.signalAll();
            if (subscribers == null) {
                return;
            }
            subs = subscribers;
            subscribers = null;
        } finally {
            bblock.unlock();
        }
        for (;;) {
            Signal.Subscriber sub = subs.poll();
            if (sub == null) {
                break;
            }
            sub.onComplete();
        }
    }

    public void blockingAwait()  {
        bblock.lock();
        try {
            while (!completed) {
                try {
                    completedCond.await();
                } catch (InterruptedException e) {
                    throw new CompletionException(e);
                }
            }
        } finally {
            bblock.unlock();
        }
    }

    public boolean blockingAwait(long timeout) {
        bblock.lock();
        try {
            long targetTime = System.currentTimeMillis()+timeout;
            for (;;) {
                if (completed) {
                    return true;
                }
                if (timeout <= 0) {
                    return false;
                }
                try {
                    completedCond.await(timeout, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    throw new CompletionException(e);
                }
                timeout = targetTime - System.currentTimeMillis();
            }
        } finally {
            bblock.unlock();
        }
    }

    public boolean blockingAwait(long timeout, TimeUnit unit) {
        bblock.lock();
        try {
            return blockingAwait(unit.toMillis(timeout));
        } finally {
            bblock.unlock();
        }
    }
}
