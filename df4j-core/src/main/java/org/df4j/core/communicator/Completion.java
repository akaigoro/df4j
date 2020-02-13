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
    protected final Condition completedCond = bblock.newCondition();
    protected Throwable completionException;
    protected LinkedList<CompletionSubscription> subscriptions = new LinkedList<>();
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
                CompletionSubscription subscription = new CompletionSubscription(co);
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

    protected void completeSubscriptions(LinkedList<CompletionSubscription> subs) {
        for (;;) {
            CompletionSubscription sub = subs.poll();
            if (sub == null) {
                break;
            }
            sub.onComplete();
        }
    }

    protected void _onComplete(Object result, Throwable e) {
        LinkedList<CompletionSubscription> subs;
        bblock.lock();
        try {
            if (completed) {
                return;
            }
            completed = true;
            setResult(result);
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
        completeSubscriptions(subs);
    }

    protected void setResult(Object result) {
    }

    /**
     * completes this {@link Completable} normally
     */
    public void onComplete() {
        _onComplete(null, null);
    }

    /**
     * completes this {@link Completable} exceptionally
     * @param e completion exception
     */
    protected void onError(Throwable e) {
        _onComplete(null, e);
    }

    /**
     * waits this {@link Completable} to complete
     */
    public void join()  throws InterruptedException  {
        bblock.lock();
        try {
            while (!completed) {
                completedCond.await();
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
            if (!completed) {
                completedCond.await(timeout, unit);
            }
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
        LinkedList<CompletionSubscription> subscribers = this.subscriptions;
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


    protected class CompletionSubscription implements SimpleSubscription {
        Completable.Observer subscriber;
        private boolean cancelled;

        protected CompletionSubscription() {
        }

        protected CompletionSubscription(Completable.Observer subscriber) {
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
            if (completionException == null) {
                subscriber.onComplete();
            } else {
                subscriber.onError(completionException);
            }
        }
    }

}
