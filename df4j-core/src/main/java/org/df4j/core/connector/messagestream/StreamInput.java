package org.df4j.core.connector.messagestream;

import org.df4j.core.connector.messagescalar.ScalarInput;
import org.df4j.core.node.AsyncTask;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * A Queue of tokens of type <T>
 *
 * @param <T>
 */
public class StreamInput<T> extends ScalarInput<T> implements StreamSubscriber<T>, Iterator<T> {
    private AsyncTask asyncTask;
    protected Deque<T> queue;
    protected boolean closeRequested = false;

    public StreamInput(AsyncTask asyncTask) {
        super(asyncTask);
        this.asyncTask = asyncTask;
        this.queue = new ArrayDeque<>();
    }

    public StreamInput(AsyncTask asyncTask, int capacity) {
        super(asyncTask);
        this.asyncTask = asyncTask;
        this.queue = new ArrayDeque<>(capacity);
    }

    public StreamInput(AsyncTask asyncTask, Deque<T> queue) {
        super(asyncTask);
        this.asyncTask = asyncTask;
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
    public synchronized void purge() {
        if (pushback) {
            pushback = false;
            return; // value remains the same, the pin remains turned on
        }
        boolean wasNull = (value == null);
        value = queue.poll();
        if (value != null) {
            return; // the pin remains turned on
        }
        // no more tokens; check closing
        if (wasNull || !closeRequested) {
            turnOff();
        }
        // else process closing: value is null, the pin remains turned on
    }

    @Override
    public boolean hasNext() {
        return value != null;
    }

    /**
     * attempt to take next token from the input queue
     *
     * @return true if next token is available, or if stream is closed false
     *         if input queue is empty
     */
    public synchronized boolean moveNext() {
        purge();
        return hasNext();
    }

    @Override
    public T next() {
        purge();
        return current();
    }

    public synchronized boolean  isClosed() {
        return closeRequested && (value == null);
    }
}
