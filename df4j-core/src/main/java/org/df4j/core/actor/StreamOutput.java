package org.df4j.core.actor;

import org.df4j.core.asyncproc.AsyncProc;
import org.df4j.core.asyncproc.SubscriptionListener;
import org.df4j.core.asyncproc.Transition;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Adds token buffer for {@link StreamSubscriptionQueue}.
 * Blocks when there are no room in the token buffer
 *
 * Each input token it transferred to only single subscriber.
 *
 * @param <T> type of tokens
 */
public class StreamOutput<T>  extends Transition.Pin
        implements SubscriptionListener<T, StreamSubscription<T>>, Publisher<T> {
    protected StreamSubscriptionQueue<T> subscriptions = new StreamSubscriptionQueue<>(this);
    protected int capacity;
    protected Queue<T> tokens;
    protected volatile boolean completionRequested = false;
    protected volatile Throwable completionException;

    public StreamOutput(AsyncProc actor, int capacity) {
        actor.super();
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
    public void subscribe(Subscriber<? super T> s) {
        subscriptions.subscribe(s);
    }

    public void onError(Throwable ex) {
        subscriptions.onError(ex);
    }

    public void onComplete() {
        subscriptions.onComplete();
        super.complete();
    }

    @Override
    public void remove(StreamSubscription<T> subscription) {
        subscriptions.remove(subscription);
    }

    @Override
    public void activate(StreamSubscription<T> subscription) {
        T token;
        synchronized (this) {
            token = tokens.poll();
        }
        if (token == null) {
            subscriptions.activate(subscription);
        } else {
            subscription.onNext(token);
            if (completionRequested) {
                if (completionException == null) {
                    subscriptions.onComplete();
                } else {
                    subscriptions.onError(completionException);
                }
            }
        }
    }

    public synchronized void onNext(T token) {
        if (token == null) {
            throw new NullPointerException();
        }
        if (completionRequested) {
            return;
        }
        StreamSubscription<T> firstSubscription = subscriptions.current();
        if (firstSubscription == null) {
            tokens.add(token);
            if (tokens.size() == capacity) {
                block();
            }
        } else {
            firstSubscription.onNext(token);
        }
    }
}
