package org.df4j.core.actor;

import org.df4j.core.Port;
import org.df4j.core.asynchproc.AsyncProc;
import org.df4j.core.asynchproc.Transition;
import org.reactivestreams.Subscription;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * A Queue of tokens
 *
 * @param <T> type of tokens
 */
public class StreamInput<T> extends Transition.Pin implements Port<T>, org.reactivestreams.Subscriber<T> {
    protected int capacity;
    protected Queue<T> tokens;
    /** to monitor existence of the room for additional tokens */
    protected Transition.Pin roomLock;
    /** extracted token */
    protected T current = null;
    protected Subscription subscription;
    protected boolean completed = false;
    protected Throwable completionException;
    protected boolean pushback = false; // if true, do not consume

    public StreamInput(AsyncProc actor, int fullCapacity) {
        actor.super();
        if (fullCapacity <= 0) {
            throw new IllegalArgumentException();
        }
        this.capacity = fullCapacity-1;
        if (fullCapacity > 0) {
            this.tokens = new ArrayDeque<>(fullCapacity);
        }
    }

    public StreamInput(AsyncProc actor) {
        this(actor, 9);
    }

    public synchronized void setRoomLockIn(AsyncProc outerActor) {
        if (this.roomLock != null) {
            throw new IllegalStateException();
        }
        this.roomLock = outerActor.new Pin(isFull());
    }

    public int size() {
        return  current == null? 0: (1+tokens.size());
    }

    public boolean isFull() {
        return  current == null? false: tokens.size() == capacity;
    }

    @Override
    public synchronized void onSubscribe(Subscription s) {
        this.subscription = s;
        s.request(capacity - size());
    }

    @Override
    public synchronized void onNext(T token) {
        if (token == null) {
            throw new NullPointerException();
        }
        if (completed) {
            throw new IllegalStateException("closed already");
        }
        if (current == null) {
            current = token;
            unblock();
        } else {
            tokens.add(token);
            if (roomLock!=null && isFull()) {
                roomLock.block();
            }
        }
    }

    public void cancel() {
        if (subscription != null) {
            subscription.cancel();
        }
    }

    /**
     * in order to reuse same token in subsequent actor step
     */
    protected void pushback() {
        if (pushback) {
            throw new IllegalStateException();
        }
        pushback = true;
    }

    /**
     * in order to reuse another token in subsequent actor step
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
            tokens.add(this.current);
            this.current = value;
            if (roomLock!=null && isFull()) {
                roomLock.block();
            }

        }
    }

    /**
     * in order to use next token in the same actor step
     *
     * @return
     */
    public synchronized boolean moveNext() {
        boolean wasFull = isFull();
        boolean wasNotNull = (current != null);
        current = tokens.poll();
        if (current == null) {
            // no more tokens for now
            if (completed) {
                return wasNotNull;
            } else {
                return false;
            }
        }
        if (roomLock!=null && wasFull) {
            roomLock.unblock();
        }
        return true;
    }

    @Override
    public  void purge() {
        int delta;
        synchronized (this) {
            int sizeBefore = size();
            if (pushback) {
                pushback = false;
            } else if (!moveNext()) {
                block();
            }
            delta = sizeBefore - size();
        }
        if (delta > 0 && subscription != null) {
            subscription.request(delta);
        }
    }

    public synchronized T next() {
        purge();
        return current;
    }

    public boolean hasNext() {
        return current != null;
    }

    protected boolean isParameter() {
        return true;
    }

    public synchronized T current() {
        return current;
    }

    public synchronized Throwable getCompletionException() {
        return completionException;
    }

    public synchronized boolean isCompleted() {
        return completed;
    }

    @Override
    public synchronized void onError(Throwable throwable) {
        onComplete();
        this.completionException = throwable;
    }

    public synchronized void onComplete() {
        if (completed) {
            return;
        }
        completed = true;
        unblock();
    }
}
