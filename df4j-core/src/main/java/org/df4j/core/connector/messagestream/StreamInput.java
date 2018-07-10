package org.df4j.core.connector.messagestream;

import org.df4j.core.connector.messagescalar.ScalarInput;
import org.df4j.core.node.AsyncTaskBase;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * A Queue of tokens of type <T>
 *
 * @param <T>
 */
public class StreamInput<T> extends ScalarInput<T> implements StreamSubscriber<T>, Iterator<T> {
    protected Deque<T> queue;
    protected boolean closeRequested = false;

    public StreamInput(AsyncTaskBase actor) {
        super(actor);
        this.queue = new ArrayDeque<>();
    }

    public StreamInput(AsyncTaskBase actor, int capacity) {
        super(actor);
        this.queue = new ArrayDeque<>(capacity);
    }

    public StreamInput(AsyncTaskBase actor, Deque<T> queue) {
        super(actor);
        this.queue = queue;
    }

    protected int size() {
        return queue.size();
    }

    @Override
    public synchronized void post(T token) {
        if (token == null) {
            throw new NullPointerException();
        }
        if (closeRequested) {
            throw new IllegalStateException("closed already");
        }
        if (exception != null) {
            throw new IllegalStateException("token set already");
        }
        if (value == null) {
            value = token;
            turnOn();
        } else {
            queue.add(token);
        }
    }

    /**
     * Signals the end of the stream. Turns this pin on. Removed value is
     * null (null cannot be send with Subscriber.add(message)).
     */
    @Override
    public synchronized void complete() {
        if (closeRequested) {
            return;
        }
        closeRequested = true;
        if (value == null) {
            turnOn();
        }
    }

    @Override
    protected void pushback() {
        if (pushback) {
            throw new IllegalStateException();
        }
        pushback = true;
    }

    @Override
    protected synchronized void pushback(T value) {
        if (value == null) {
            throw new IllegalArgumentException();
        }
        if (!pushback) {
            pushback = true;
        } else {
            if (this.value == null) {
                throw new IllegalStateException();
            }
            queue.addFirst(this.value);
            this.value = value;
        }
    }

    @Override
    public synchronized T next() {
        if (pushback) {
            pushback = false;
            return value; // value remains the same, the pin remains turned on
        }
        T res = value;
        boolean wasNull = (value == null);
        value = queue.poll();
        if (value == null) {
            // no more tokens; check closing
            if (wasNull || !closeRequested) {
                turnOff();
            }
        }
        return res;
    }

    @Override
    public boolean hasNext() {
        return value != null;
    }

    public synchronized boolean  isClosed() {
        return closeRequested && (value == null);
    }
}
