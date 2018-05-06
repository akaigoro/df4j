package org.df4j.core.core.connector.reactivestream;

import org.df4j.core.core.connector.messagestream.StreamInput;
import org.df4j.core.core.node.AsyncTask;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * A Queue of tokens of type <T>
 *
 * @param <T>
 */
public class ReactiveInput<T> extends StreamInput<T> implements Subscriber<T>, Iterator<T> {
    private AsyncTask asyncTask;
    protected Deque<T> queue;
    protected boolean closeRequested = false;
    protected int freeSpace;
    protected Subscription subscription;

    public ReactiveInput(AsyncTask asyncTask, int capacity) {
        super(asyncTask);
        this.asyncTask = asyncTask;
        this.queue = new ArrayDeque<>(capacity);
        freeSpace = capacity;
    }

    public ReactiveInput(AsyncTask asyncTask) {
        this(asyncTask, 8);
    }

    protected int size() {
        return queue.size();
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscription.request(freeSpace);
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
        freeSpace--;
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
            // only single pushback is allowed
            throw new IllegalStateException();
        }
        pushback = true;
    }

    @Override
    protected synchronized void pushback(T value) {
        if (value == null) {
            throw new IllegalArgumentException();
        }
        if (pushback) {
            // only single pushback is allowed
            throw new IllegalStateException();
        }
        pushback = true;
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
