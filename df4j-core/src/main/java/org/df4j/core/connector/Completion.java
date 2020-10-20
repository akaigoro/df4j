package org.df4j.core.connector;

import org.df4j.protocol.Completable;

import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

/**
 * Completes successfully or with failure, without emitting any value.
 * Similar to {@link CompletableFuture}&lt;Void&gt;
 */
public class Completion implements CompletionI {
    protected Throwable completionException;
    protected LinkedList<CompletionSubscription> subscriptions = new LinkedList<>();
    protected boolean completed;

    public void setCompletionException(Throwable completionException) {
        this.completionException = completionException;
    }

    /**
     * @return completion Exception, if this {@link Completable} was completed exceptionally;
     *         null otherwise
     */
    public synchronized Throwable getCompletionException() {
        return completionException;
    }

    public LinkedList<CompletionSubscription> getSubscriptions() {
        return subscriptions;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    /**
     * @return true if this {@link Completable} was completed normally or exceptionally;
     *         false otherwise
     */
    public synchronized boolean isCompleted() {
        return completed;
    }

    public void subscribe(Completable.Observer co) {
        synchronized(this) {
            if (!completed) {
                CompletionSubscription subscription = new CompletionSubscription(this, co);
                subscriptions.add(subscription);
                co.onSubscribe(subscription);
                return;
            }
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

    protected void _complete(Throwable e) {
        LinkedList<CompletionSubscription> subs;
        synchronized(this) {
            if (completed) {
                return;
            }
            completed = true;
            this.completionException = e;
            notifyAll();
            if (subscriptions == null) {
                return;
            }
            subs = subscriptions;
            subscriptions = null;
        }
        completeSubscriptions(subs);
    }

    /**
     * completes this {@link Completable} normally
     */
    public void complete() {
        whenComplete();
        _complete(null);
    }

    protected void whenComplete() {
    }

    protected void whenError(Throwable e) {
        whenComplete();
    }

    /**
     * If not already completed, causes invocations of get() and related methods
     * to throw {@link CompletionException}
     * with the given exception as the cause.
     * @param e exception to throw
     */
    public void completeExceptionally(Throwable e) {
        if (e == null) {
            throw new IllegalArgumentException();
        }
        whenError(e);
        _complete(e);
    }

    /**
     * waits this {@link Completable} to complete
     */
    public synchronized void await() {
        try {
            while (!completed) {
                wait();
            }
            if (completionException != null) {
                throw new CompletionException(completionException);
            }
        } catch (InterruptedException ie) {
            throw new CompletionException(ie);
        }
    }

    /**
     * waits this {@link Completable} to complete until timeout
     * @param timeoutMillis timeout in millisecomds
     * @return true if completed;
     *         false if timout reached
     */
    public synchronized boolean await(long timeoutMillis) {
        long targetTime = System.currentTimeMillis()+timeoutMillis;
        try {
            for (;;) {
                if (completed) {
                    if (completionException == null) {
                        return true;
                    } else {
                        throw new CompletionException(completionException);
                    }
                }
                if (timeoutMillis <= 0) {
                    return false;
                }
                wait(timeoutMillis);
                timeoutMillis = targetTime - System.currentTimeMillis();
            }
        } catch (InterruptedException e) {
            throw new CompletionException(e);
        }
    }

    public synchronized boolean await(long timeout, TimeUnit unit) {
        long timeoutMillis = unit.toMillis(timeout);
        return await(timeoutMillis);
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

}
