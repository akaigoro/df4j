package org.df4j.core.actor;

import java.util.Deque;
import java.util.LinkedList;

import org.df4j.core.StreamPort;

/**
 * A Queue of tokens of type <T>
 * 
 * @param <T>
 */
public class StreamInput<T> extends Input<T> implements StreamPort<T> {
    private Deque<T> queue;
    private boolean closeRequested = false;

    public StreamInput (Actor actor) {
        super(actor);
        this.queue = new LinkedList<T>();
    }

    public StreamInput(Actor actor, Deque<T> queue) {
        super(actor);
        this.queue = queue;
    }

    public T get() {
        return value;
    }

    @Override
    public void post(T token) {
        if (token == null) {
            throw new NullPointerException();
        }
        boolean doFire;
        gate.lock.lock();
        try {
            if (closeRequested) {
                overflow(token);
            }
            if (value == null) {
                value = token;
                doFire = turnOn();
            } else {
                add(token);
                return; // is On already
            }
        } finally {
            gate.lock.unlock();
        }
        if (doFire) {
            gate.fire();
        }
    }

    protected void overflow(T token) {
        throw new IllegalStateException("closed already");
    }

    protected void add(T token) {
        queue.add(token);
    }

    @Override
    public T poll() {
        return queue.poll();
    }

    /**
     * Signals the end of the stream. Turns this pin on. Removed value is
     * null (null cannot be send with StreamInput.add(message)).
     */
    @Override
    public void close() {
        boolean doFire;
        gate.lock.lock();
        try {
            if (closeRequested) {
                return;
            }
            closeRequested = true;
            // System.out.println("close()");
            doFire = turnOn();
        } finally {
            gate.lock.unlock();
        }
        if (doFire) {
            gate.fire();
        }
    }

    @Override
    public void pushback() {
        if (pushback) {
            throw new IllegalStateException();
        }
        pushback = true;
    }

    @Override
    protected void pushback(T value) {
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

    /**
     * attempt to take next token from the input queue
     * 
     * @return true if next token is available, or if stream is closed false
     *         if input queue is empty
     */
    public boolean moveNext() {
        gate.lock.lock();
        try {
            if (pushback) {
                pushback = false;
                return true;
            }
            boolean wasNotNull = (value != null);
            T newValue = queue.poll();
            if (newValue != null) {
                value = newValue;
                return true;
            } else if (closeRequested) {
                value = null;
                return wasNotNull;// after close, return true once, then
                                    // false
            } else {
                return false;
            }
        } finally {
            gate.lock.unlock();
        }
    }

    @Override
    protected boolean consume() {
        if (pushback) {
            pushback = false;
            // value remains the same, the pin remains turned on
            return true;
        }
        boolean wasNotNull = (value != null);
        value = poll();
        if (value != null) {
            return true; // continue processing
        }
        // no more tokens; check closing
        return wasNotNull && closeRequested;
        // else process closing: value is null, the pin remains turned on
    }

    public boolean isClosed() {
        gate.lock.lock();
        try {
            return closeRequested && (value == null);
        } finally {
            gate.lock.unlock();
        }
    }
}