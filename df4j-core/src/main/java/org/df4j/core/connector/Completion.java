package org.df4j.core.connector;

import org.df4j.protocol.Completable;
import org.df4j.protocol.SimpleSubscription;

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

    protected LinkedList<CompletionSubscription> getSubscriptions() {
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

    public void subscribe(Completable.Subscriber co) {
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
        _complete(null);
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
        _complete(e);
    }

    /**
     * waits this {@link Completable} to complete
     */
    public synchronized void await() throws InterruptedException {
        while (!completed) {
            wait();
        }
        if (completionException != null) {
            throw new CompletionException(completionException);
        }
    }

    /**
     * waits this {@link Completable} to complete until timeout
     * @param timeoutMillis timeout in millisecomds
     * @return true if completed;
     *         false if timout reached
     */
    public synchronized boolean await(long timeoutMillis) throws InterruptedException {
        long targetTime = System.currentTimeMillis()+timeoutMillis;
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
    }

    public synchronized boolean await(long timeout, TimeUnit unit) throws InterruptedException {
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

    static class CompletionSubscription implements SimpleSubscription {
        private final Completion completion;
        Completable.Subscriber subscriber;
        private boolean cancelled;

        protected CompletionSubscription(Completion completion, Completable.Subscriber subscriber) {
            this.completion = completion;
            this.subscriber = subscriber;
        }

        @Override
        public void cancel() {
            synchronized (completion) {
                if (cancelled) {
                    return;
                }
                cancelled = true;
                LinkedList<CompletionSubscription> subscriptions = completion.getSubscriptions();
                if (subscriptions != null) {
                    subscriptions.remove(this);
                }
            }
        }

        @Override
        public boolean isCancelled() {
            synchronized (completion) {
                return cancelled;
            }
        }

        void onComplete() {
            Throwable completionException = completion.getCompletionException();
            if (completionException == null) {
                subscriber.onComplete();
            } else {
                subscriber.onError(completionException);
            }
        }
    }
}
