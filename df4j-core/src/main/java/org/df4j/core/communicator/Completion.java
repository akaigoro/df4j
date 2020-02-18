package org.df4j.core.communicator;

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
public class Completion implements Completable.Source {
    protected Throwable completionException;
    protected LinkedList<CompletionSubscription> subscriptions = new LinkedList<>();
    protected boolean completed;

    /**
     * @return completion Exception, if this {@link Completable} was completed exceptionally;
     *         null otherwise
     */
    public synchronized Throwable getCompletionException() {
        return completionException;
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
                CompletionSubscription subscription = new CompletionSubscription(co);
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

    protected void _onComplete(Object result, Throwable e) {
        LinkedList<CompletionSubscription> subs;
        synchronized(this) {
            if (completed) {
                return;
            }
            completed = true;
            setResult(result);
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
     * @throws InterruptedException if this thread interrupted
     */
    public void join()  throws InterruptedException  {
        synchronized(this) {
            while (!completed) {
                notifyAll();
            }
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
    public synchronized boolean blockingAwait(long timeoutMillis) {
        long targetTime = System.currentTimeMillis()+timeoutMillis;
        try {
            while (!completed && timeoutMillis > 0) {
                wait(timeoutMillis);
                timeoutMillis = targetTime - System.currentTimeMillis();
            }
            if (completed && completionException != null) {
                throw new CompletionException(completionException);
            }
            return completed;
        } catch (InterruptedException e) {
            throw new CompletionException(e);
        }
    }

    /**
     * waits this {@link Completable} to complete until timeout
     * @param timeout timeout in units
     * @param unit time unit
     * @return true if completed;
     *         false if timout reached
     */
    public synchronized boolean blockingAwait(long timeout, TimeUnit unit) {
        long timeoutMillis = unit.toMillis(timeout);
        return blockingAwait(timeoutMillis);
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
            synchronized(Completion.this) {
                if (cancelled) {
                    return;
                }
                cancelled = true;
                if (subscriptions != null) {
                    subscriptions.remove(this);
                }
            }
        }

        @Override
        public boolean isCancelled() {
            synchronized(Completion.this) {
                return cancelled;
            }
        }

        void onComplete() {
            if (completionException == null) {
                subscriber.onComplete();
            } else {
                subscriber.onError(completionException);
            }
        }
    }

}
