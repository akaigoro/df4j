package org.df4j.core.actor;

import org.df4j.core.SubscriptionCancelledException;
import org.df4j.core.asyncproc.*;
import org.reactivestreams.Publisher;

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
public class StreamOutput<T> extends StreamSubscriptionQueue<T> implements Publisher<T> {

    private final StreamLock streamLock;
    protected int capacity;
    protected Queue<T> tokens;

    public StreamOutput(AsyncProc actor, int capacity) {
        streamLock = new StreamLock(actor);
        streamLock.unblock();
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
    protected boolean hasNextToken() {
        return !tokens.isEmpty();
    }

    @Override
    protected T nextToken() {
        T t = tokens.poll();
        if (t != null) {
            streamLock.unblock();
        }
        return t;
    }

    public void onNext(T token) {
        if (token == null) {
            throw new NullPointerException();
        }
        locker.lock();
        try {
            if (completionRequested) {
                return;
            }
            if (tokens.size() >= capacity) {
                throw new IllegalStateException("buffer overflow");
            }
            tokens.add(token);
            if (tokens.size() >= capacity) {
                streamLock.block();
            }
            matchingLoop();
        } finally {
            locker.unlock();
        }
    }

}
