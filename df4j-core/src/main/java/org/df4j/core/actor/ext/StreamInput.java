package org.df4j.core.actor.ext;

import org.df4j.core.Port;
import org.df4j.core.asynchproc.AsyncProc;
import org.df4j.core.asynchproc.ScalarInput;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * A Queue of tokens
 *
 * @param <T> type of tokens
 */
public class StreamInput<T> extends ScalarInput<T> implements Port<T> {
    protected Queue<T> queue;
    protected boolean closeRequested = false;

    public StreamInput(AsyncProc actor) {
        super(actor);
        this.queue = new ArrayDeque<>();
    }

    public StreamInput(AsyncProc actor, int capacity) {
        super(actor);
        this.queue = new ArrayDeque<>(capacity);
    }

    public StreamInput(AsyncProc actor, Queue<T> queue) {
        super(actor);
        this.queue = queue;
    }

    public int size() {
        return queue.size();
    }

    @Override
    public synchronized void onNext(T token) {
        if (token == null) {
            throw new NullPointerException();
        }
        if (closeRequested) {
            throw new IllegalStateException("closed already");
        }
        if (exception != null) {
            throw new IllegalStateException("token set already");
        }
        if (current == null) {
            current = token;
            unblock();
        } else {
            queue.add(token);
        }
    }

    /**
     * Signals the end of the stream. Turns this pin on. Removed value is
     * null (null cannot be send with Subscriber.add(message)).
     */
    @Override
    public synchronized void onComplete() {
        if (closeRequested) {
            return;
        }
        closeRequested = true;
        if (current == null) {
            unblock();
        }
    }

    /**
     * in order to reuse same token in subsequent async call
     */
    protected void pushback() {
        if (pushback) {
            throw new IllegalStateException();
        }
        pushback = true;
    }

    /**
     * in order to reuse another token in subsequent async call
     */
    protected synchronized void pushback(T value) {
        if (value == null) {
            throw new IllegalArgumentException();
        }
        if (!pushback) {
            pushback = true;
        } else {
            if (this.current == null) {
                throw new IllegalStateException();
            }
            queue.add(this.current);
            this.current = value;
        }
    }

    /**
     * in order to use next token in the same async call
     *
     * @return
     */
    public boolean moveNext() {
        boolean wasNotNull = (current != null);
        current = queue.poll();
        return current != null || (closeRequested && wasNotNull);
    }

    @Override
    public synchronized void purge() {
        if (pushback) {
            pushback = false;
        } else if (!moveNext()) {
            block();
        }
    }

    @Override
    public synchronized T next() {
        purge();
        return current();
    }

    @Override
    public boolean hasNext() {
        return current != null;
    }

    public synchronized boolean  isClosed() {
        return closeRequested && (current == null);
    }
}
