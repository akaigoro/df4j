package org.df4j.core.communicator;

import org.df4j.protocol.Completable;
import org.df4j.protocol.SimpleSubscription;

import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Completes successfully or with failure, without emitting any value.
 * Similar to {@link CompletableFuture}&lt;Void&gt;
 */
public class Completion implements Completable.Source {
    protected final Lock bblock = new ReentrantLock();
    private final Condition completedCond = bblock.newCondition();
    protected Throwable completionException;
    protected LinkedList<Subscription> subscriptions = new LinkedList<>();
    protected boolean completed;

    /**
     * @return completion Exception, if this {@link Completable} was completed exceptionally;
     *         null otherwise
     */
    public Throwable getCompletionException() {
        bblock.lock();
        try {
            return completionException;
        } finally {
            bblock.unlock();
        }
    }

    /**
     * @return true if this {@link Completable} was completed normally or exceptionally;
     *         false otherwise
     */
    public boolean isCompleted() {
        bblock.lock();
        try {
            return completed;
        } finally {
            bblock.unlock();
        }
    }

    public void subscribe(Completable.Observer co) {
        bblock.lock();
        try {
            if (!completed) {
                Subscription subscription = new Subscription(co);
                subscriptions.add(subscription);
                co.onSubscribe(subscription);
                return;
            }
        } finally {
            bblock.unlock();
        }
        if (getCompletionException() == null) {
            co.onComplete();
        } else {
            Throwable completionException = getCompletionException();
            co.onError(completionException);
        }
    }

    /**
     * completes this {@link Completable} exceptionally
     * @param e completion exception
     */
    protected void onError(Throwable e) {
        LinkedList<Subscription> subs;
        bblock.lock();
        try {
            if (completed) {
                return;
            }
            completed = true;
            this.completionException = e;
            completedCond.signalAll();
            if (subscriptions == null) {
                return;
            }
            subs = subscriptions;
            subscriptions = null;
        } finally {
            bblock.unlock();
        }
        for (;;) {
            Subscription sub = subs.poll();
            if (sub == null) {
                break;
            }
            if (e == null) {
                sub.onComplete();
            } else {
                sub.onError(e);
            }
        }
    }

    /**
     * completes this {@link Completable} normally
     */
    public void onComplete() {
        onError(null);
    }

    /**
     * waits this {@link Completable} to complete
     */
    public void join() {
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
        if (completionException != null) {
            throw new CompletionException(completionException);
        }
    }

    /**
     * waits this {@link Completable} to complete until timeout
     * @param timeout timeout in millisecomds
     * @return true if completed;
     *         false if timout reached
     */
    public boolean blockingAwait(long timeout) {
       return blockingAwait(timeout, TimeUnit.MILLISECONDS);
    }

    /**
     * waits this {@link Completable} to complete until timeout
     * @param timeout timeout in units
     * @param unit time unit
     * @return true if completed;
     *         false if timout reached
     */
    public boolean blockingAwait(long timeout, TimeUnit unit) {
        bblock.lock();
        try {
            if (!isCompleted()) {
                completedCond.await(timeout, unit);
            }
            boolean completed = isCompleted();
            if (completed && completionException != null) {
                throw new CompletionException(completionException);
            }
            return completed;
        } catch (InterruptedException e) {
            throw new CompletionException(e);
        } finally {
            bblock.unlock();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());sb.append(" ");
        LinkedList<Subscription> subscribers = this.subscriptions;
        Throwable completionException = this.completionException;
        int size = 0;
        if (subscribers!=null) {
            size=subscribers.size();
        }
        if (!completed) {
            sb.append("not completed; subscribers: "+size);
        } else if (completionException == null) {
            sb.append("completed successfully");
        } else {
            sb.append("completed with exception: ");
            sb.append(completionException.toString());
        }
        return sb.toString();
    }


    protected class Subscription implements SimpleSubscription {
        Completable.Observer subscriber;
        private boolean cancelled;

        protected Subscription() {
        }

        protected Subscription(Completable.Observer subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void cancel() {
            bblock.lock();
            try {
                if (cancelled) {
                    return;
                }
                cancelled = true;
                if (subscriptions != null) {
                    subscriptions.remove(this);
                }
            } finally {
                bblock.unlock();
            }
        }

        @Override
        public boolean isCancelled() {
            bblock.lock();
            try {
                return cancelled;
            } finally {
                bblock.unlock();
            }
        }

        public void onComplete() {
            subscriber.onComplete();
        }

        public void onError(Throwable e) {
            subscriber.onError(e);
        }
    }

}
