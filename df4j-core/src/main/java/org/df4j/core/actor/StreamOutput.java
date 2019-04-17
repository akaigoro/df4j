package org.df4j.core.actor;

import org.df4j.core.actor.ext.SyncActor;
import org.df4j.core.asyncproc.AsyncProc;
import org.df4j.core.asyncproc.Transition;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Non-blocking analogue of blocking queue.
 * Serves multiple consumers (subscribers)
 *
 * Each message is routed to exactly one subscriber.
 * Has limited buffer for messages.  When toverflows, his buffer overflows, {@link StreamOutput#outerLock} blocks.
 *
 * @param <T> the type of transferred messages
 *
 *  Though it extends {@link Actor}, it is a connector and not an independent node.
 */
public class StreamOutput<T> extends SyncActor implements Publisher<T> {
    protected StreamInput<T> tokens = new StreamInput<>(this);
    protected StreamSubscriptionBlockingQueue<T> subscriptions = new StreamSubscriptionBlockingQueue<>(this);

    protected final Transition.Pin outerLock;
    protected final int capacity;

    public StreamOutput(AsyncProc outerActor, int capacity) {
        outerLock = outerActor.new Pin(false);
        if (capacity <= 0) {
            throw new IllegalArgumentException();
        }
        this.capacity = capacity;
        start();
    }

    public StreamOutput(AsyncProc actor) {
        this(actor, 16);
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
    protected void runAction() throws Throwable {
        T token = tokens.current();
        if (token != null) {
            StreamSubscription subscription = subscriptions.current();
            subscription.onNext(token);
        } else if (!tokens.isCompleted()) {
            throw new RuntimeException("tokens ampty and not completed, but unblocked");
        } else {
            subscriptions.complete(tokens.getCompletionException());
        }
    }
}
