package org.df4j.core.actor;

import org.df4j.core.Port;
import org.df4j.core.asyncproc.AsyncProc;
import org.df4j.core.asyncproc.Transition;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Non-blocking analogue of blocking queue.
 * Serves multiple consumers (subscribers)
 * demonstrates usage of class {@link Semafor} for handling back pressure?
 *
 * Each message is routed to exactly one subscriber. When no one subscriber exists, blocks.
 * Has limited buffer for messages.
 *
 * @param <T> the type of transferred messages
 *
 *  Though it extends {@link Actor}, it is a connector and not an independent node.
 */
public class StreamOutput<T> extends Actor implements Port<T>, Publisher<T> {
    protected StreamInput<T> tokens = new StreamInput<>(this);
    protected StreamSubscriptionBlockingQueue subscriptions = new StreamSubscriptionBlockingQueue(this);

    protected final Transition.Pin outerLock;
    protected final int capacity;
    protected boolean completed = false;
    protected Throwable completionException = null;

    public StreamOutput(AsyncProc outerActor, int capacity) {
        outerLock = outerActor.new Pin(true);
        this.capacity = capacity;
    }

    public StreamOutput(AsyncProc actor) {
        this(actor, 16);
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        StreamSubscription<T> newSubscription = new StreamSubscription(subscriptions, subscriber);
        subscriber.onSubscribe(newSubscription);
    }

    public synchronized boolean isCompleted() {
        return completed;
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

    public synchronized void onComplete() {
        if (completed) {
            return; // completed already
        }
        completed = true;
        outerLock.block();
    }

    @Override
    public synchronized void onError(Throwable throwable) {
        completionException = throwable;
        onComplete();
    }

    AtomicBoolean working = new AtomicBoolean(false);

    @Override
    protected void runAction() throws Throwable {
        T token = tokens.current();
        StreamSubscription subscription = subscriptions.current();
        if (token != null) {
            subscription.onNext(token);
        } else if (!tokens.completed) {
            throw new RuntimeException();
        } else if (tokens.completionException == null) {
            subscription.onComplete();
        } else {
            subscription.onError(tokens.completionException);
        }
    }
}
