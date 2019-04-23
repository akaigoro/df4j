package org.df4j.core.actor;

import org.df4j.core.actor.ext.SyncActor;
import org.df4j.core.asyncproc.AsyncProc;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.concurrent.CancellationException;

/**
 * Non-blocking analogue of blocking queue.
 * Serves multiple consumers (subscribers)
 *
 * Each message is routed to exactly one subscriber.
 * Has limited buffer for messages.  When the buffer overflows, this {@link StreamOutput#outerLock} blocks.
 *
 * @param <T> the type of transferred messages
 *
 *  Though it extends {@link Actor}, it is a connector and not an independent node.
 */
public class StreamOutput<T> extends SyncActor implements Publisher<T> {
    protected StreamInput<T> tokens = new StreamInput<>(this);
    protected StreamSubscriptionConnector<T> subscriptions = new StreamSubscriptionConnector<>(this);

    protected final StreamLock outerLock;
    protected final int capacity;

    public StreamOutput(AsyncProc outerActor, int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException();
        }
        this.capacity = capacity;
        outerLock = new StreamLock(outerActor);
        outerLock.unblock();
        start();
    }

    public StreamOutput(AsyncProc outerActor) {
        this(outerActor, 16);
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        subscriptions.subscribe(subscriber);
    }

    public synchronized void onNext(T item) {
        if (tokens.size() >= capacity) {
            throw new IllegalStateException("buffer overflow");
        }
        tokens.onNext(item);
        if (tokens.size() >= capacity) {
            outerLock.block();
        }
    }

    public void complete(Throwable completionException) {
        tokens.complete(completionException);
    }

    public void onComplete() {
        tokens.onComplete();
    }

    public synchronized void onError(Throwable throwable) {
        tokens.onError(throwable);
    }

    @Override
    protected void runAction() {
        if (tokens.isCompleted()) {
            subscriptions.complete(tokens.getCompletionException());
            return;
        }
        try {
            subscriptions.onNext(tokens.current);
        } catch (CancellationException e) {
            // subscription cancelled while being current
            tokens.pushBack();
        }
    }
}
