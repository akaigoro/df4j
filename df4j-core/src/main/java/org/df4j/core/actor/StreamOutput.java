package org.df4j.core.actor;

import org.df4j.core.SubscriptionCancelledException;
import org.df4j.core.asyncproc.*;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Non-blocking analogue of blocking queue.
 * Serves multiple consumers (subscribers)
 *
 * Each message is routed to exactly one subscriber.
 * Has limited buffer for messages.  When the buffer overflows, this {@link StreamOutput#outerLock} blocks.
 *
 * @param <T> the type of transferred messages
 *
 */
public class StreamOutput<T> extends StreamLock implements Publisher<T> {

    protected int capacity;
    protected Queue<T> tokens;
    protected boolean completionRequested = false;
    protected boolean completed = false;
    protected T current;
    protected Throwable completionException;

    /** place for demands */
    protected StreamSubscriptionQueue<T> subscriptions = new StreamSubscriptionQueue<>();

    public StreamOutput(AsyncProc actor, int capacity) {
        super(actor);
        if (capacity <= 0) {
            throw new IllegalArgumentException();
        }
        this.capacity = capacity;
        this.tokens = new ArrayDeque<>(capacity);
    }

    public StreamOutput(AsyncProc outerActor) {
        this(outerActor, 16);
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        subscriptions.subscribe(subscriber);
    }

    public synchronized void onNext(T token) {
        if (token == null) {
            throw new NullPointerException();
        }
        for (;;) {
            StreamSubscription<T> subscr;
            synchronized(this) {
                if (completionRequested) {
                    return;
                }
                subscr = subscriptions.poll();
                if (subscr == null) {
                    if (tokens.size() >= capacity) {
                        throw new IllegalStateException("buffer overflow");
                    }
                    tokens.add(token);
                    if (tokens.size() >= capacity) {
                        super.block();
                    }
                    return;
                }
            }
            try {
                subscr.onNext(token);
                return;
            } catch (SubscriptionCancelledException e) {
                // subscription can be cancelled after exiting this synchronized block
                // and before the call to  subscr.onNext(token);
                // in t6his case we try to pass the token to the next subscription
                // in order not to lost the token
            }
        }
    }

    protected void completeInput(Throwable throwable) {
        if (throwable != null) {
            throw  new IllegalArgumentException();
        }
        synchronized(this) {
            if (completionRequested) {
                return;
            }
            completionRequested = true;
            this.completionException = throwable;
            if (tokens.isEmpty()) {
                completed = true;
                return;
            }
        }
    }

    public void onComplete() {
        completeInput(null);
        subscriptions.onComplete();
    }

    public void onError(Throwable throwable) {
        completeInput(throwable);
        subscriptions.onError(throwable);
    }

    public void completion(Throwable completionException) {
        completeInput(completionException);
        subscriptions.completion(completionException);
    }
}
