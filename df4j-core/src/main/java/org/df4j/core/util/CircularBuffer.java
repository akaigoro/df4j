package org.df4j.core.util;

import java.util.ArrayDeque;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletionException;

public class CircularBuffer<T> extends ArrayDeque<T> {
    private final int capacity;
    private Throwable completionException;
    protected volatile boolean completed;

    public CircularBuffer(int capacity) {
        super(capacity);
        this.capacity = capacity;
    }

    public void addLast(T message) {
        if (completed) {
            return;
        }
        int sizeBefore = size();
        if (sizeBefore == capacity) {
            throw new IllegalStateException("CircularBuffer: full");
        }
        super.addLast(message);
        if (sizeBefore == 0) {
            stateHasTokens();
        }
        if (sizeBefore == capacity - 1) {
            stateNoRoom();
        }
    }

    public void onComplete() {
        onError(null);
    }

    public void onError(Throwable ex) {
        if (completed) {
            return;
        }
        completed = true;
        completionException = ex;
        completed = true;
        if (size() == 0) {
            stateHasTokens();
        }
    }

    public boolean isCompleted() {
        return completed == true && size() == 0;
    }

    public Throwable getCompletionException() {
        return completionException;
    }

    public T pollFirst() {
        int sizeBefore = size();
        T res = super.pollFirst();
        if (sizeBefore == 1) {
            stateNoTokens();
        }
        if (sizeBefore == capacity) {
            stateHasRoom();
        }
        return res;
    }

    public T remove() {
        T res = pollFirst();
        if (res != null) {
            return res;
        }
        if (completed) {
            throw new CompletionException(completionException);
        } else {
            throw new NoSuchElementException();
        }
    }

    protected void stateNoTokens() {}
    protected void stateHasTokens() {}
    protected void stateNoRoom() {}
    protected void stateHasRoom() {}
}