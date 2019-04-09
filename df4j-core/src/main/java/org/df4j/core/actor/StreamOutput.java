package org.df4j.core.actor;

import org.df4j.core.Port;
import org.df4j.core.asyncproc.AsyncProc;
import org.reactivestreams.Publisher;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Adds token buffer for {@link StreamSubscriptionBlockingQueue}.
 * Blocks when there are no room in the token buffer
 *
 * Each input token it transferred to only single subscriber.
 *
 * @param <T> type of tokens
 */
public class StreamOutput<T> extends StreamSubscriptionBlockingQueue<T> implements Port<T>, Publisher<T> {
    protected int capacity;
    protected Queue<T> tokens;
    protected volatile boolean completionRequested = false;
    protected volatile Throwable completionException;

    public StreamOutput(AsyncProc actor, int capacity) {
        super(actor);
        if (capacity <= 0) {
            throw new IllegalArgumentException();
        }
        this.capacity = capacity;
        this.tokens = new ArrayDeque<>(capacity);
        unblock(); // tokens have room
    }

    public StreamOutput(AsyncProc actor) {
        this(actor, 16);
    }

    @Override
    public void serveRequest(StreamSubscription<T> subscription) {
        T token;
        synchronized (this) {
            token = tokens.poll();
        }
        if (token == null) {
            super.serveRequest(subscription);
        } else {
            subscription.onNext(token);
            if (completionRequested) {
                if (completionException == null) {
                    super.onComplete();
                } else {
                    super.onError(completionException);
                }
            }
        }
    }

    protected boolean isFull() {
        return tokens.size() == capacity;
    }

    public void onNext(T token) {
        if (token == null) {
            throw new NullPointerException();
        }
        if (completionRequested) {
            return;
        }
        StreamSubscription<T> firstSubscription = super.current();
        if (firstSubscription == null) {
            tokens.add(token);
            if (isFull()) {
                block();
            }
        } else {
            firstSubscription.onNext(token);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        if (throwable == null) {
            throw new NullPointerException();
        }
        synchronized (this) {
            if (completionRequested) {
                return;
            }
            completionRequested = true;
            completionException = throwable;
            if (!tokens.isEmpty()) {
                return;
            }
        }
        super.onError(throwable);
    }

    public synchronized void onComplete() {
        synchronized (this) {
            if (completionRequested) {
                return;
            }
            completionRequested = true;
            if (!tokens.isEmpty()) {
                return;
            }
        }
        super.onComplete();
    }
}
