package org.df4j.core.boundconnector.messagestream;

import org.df4j.core.boundconnector.messagescalar.ScalarInput;
import org.df4j.core.tasknode.AsyncProc;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Iterator;

/**
 * A Queue of tokens
 *
 * @param <T> type of tokens
 */
public class StreamInput<T> extends ScalarInput<T> implements Iterator<T>, StreamSubscriber<T> {
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
            queue.add(this.value);
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
