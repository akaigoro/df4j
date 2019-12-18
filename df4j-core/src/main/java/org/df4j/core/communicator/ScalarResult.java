package org.df4j.core.communicator;

import org.df4j.core.protocol.ScalarMessage;

import java.util.LinkedList;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * {@link ScalarResult} can be considered as a one-shot {@link AsyncArrayQueue}: once set, it always satisfies {@link ScalarResult#subscribe(ScalarMessage.Subscriber)}
 *
 * Universal standalone connector for scalar values.
 * Has synchronous (Future), and asynchronous (both for scalar and stream kinds of subscribers)
 * interfaces on output end.
 *
 */
public class ScalarResult<T> implements ScalarMessage.Subscriber<T>, ScalarMessage.Publisher<T>, Future<T> {
    protected CompletableObservers<ScalarMessage.Subscriber> subscribers;
    protected volatile T value;

    public ScalarResult(CompletableObservers<ScalarMessage.Subscriber> subscribers) {
        this.subscribers = subscribers;
    }

    public ScalarResult() {
        this(new CompletableObservers<>());
    }

    public ScalarResult(CompletionStage<? extends T> completionStage) {
        completionStage.whenComplete((value, ex)->{
            if (ex == null) {
                onSuccess(value);
            } else {
                onError(ex);
            }
        });
    }
    /**
     *
     * @return true if not subscribed
     */
    @Override
    public synchronized boolean isCancelled() {
        return false;
    }

    @Override
    public synchronized boolean isDone() {
        return subscribers.completed;
    }

    @Override
    public synchronized void onSuccess(T t) {
        LinkedList<ScalarMessage.Subscriber> subs;
        synchronized(this) {
            if (subscribers.completed) { // this is how CompletableFuture#complete works
                return;
            }
            subscribers.completed = true;
            value = t;
            notifyAll();
            subs = subscribers.removeSubscribers();
            if (subs == null) {
                return;
            }
        }
        for (;;) {
            ScalarMessage.Subscriber sub = subs.poll();
            if (sub == null) {
                break;
            }
            sub.onSuccess(t);
        }
    }

    public void subscribe(ScalarMessage.Subscriber<? super T> s) {
        if (s == null) {
            throw new NullPointerException();
        }
        synchronized(this) {
            if (!subscribers.completed) {
                subscribers.subscribe(s);
                return;
            }
        }
        if (subscribers.completionException == null) {
            s.onSuccess(value);
        } else {
            s.onError(subscribers.completionException);
        }
    }

    @Override
    public synchronized boolean unsubscribe(ScalarMessage.Subscriber<T> s) {
        if (s == null) {
            throw new NullPointerException();
        }
        return subscribers.unsubscribe(s);
    }

    /**
     * Cancels subscription, and not the task, as interfece {@link Future} assumes.
     * @param mayInterruptIfRunning not used
     * @return false if was not subscribed
     *         true if was subscribed and subscription cancelled
     */
    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public synchronized T get() {
        subscribers.blockingAwait();
        if (subscribers.completionException == null) {
            return value;
        } else {
            throw new CompletionException(subscribers.completionException);
        }
    }

    @Override
    public synchronized T get(long timeout, TimeUnit unit) {
        subscribers.blockingAwait(timeout, unit);
        if (subscribers.completionException == null) {
            return value;
        } else {
            throw new CompletionException(subscribers.completionException);
        }
    }

    public static <U> ScalarResult<U> completedResult(U value) {
        ScalarResult<U> result = new ScalarResult<>();
        result.onSuccess(value);
        return result;
    }

}
