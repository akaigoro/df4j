package org.df4j.core.actor;

import org.df4j.core.asyncproc.AsyncProc;
import org.df4j.core.asyncproc.ScalarLock;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * A Queue of tokens
 *
 * blocks when there are no input tokens in the input buffer,
 * and also can manage additional {@link ScalarLock} blocked when the buffer is full.
 *
 * @param <T> type of tokens
 */
public class StreamInput<T> extends StreamParameter<T> implements Subscriber<T> {
    protected int capacity;
    protected Queue<T> tokens;
    /** to monitor existence of the room for additional tokens */
    protected StreamLock roomLock;
    /** extracted token */
    protected Subscription subscription;
    protected boolean completionRequested = false;
    protected boolean completed = false;
    protected T current;
    protected Throwable completionException;
    protected boolean pushback = false;

    public StreamInput(AsyncProc actor, int fullCapacity) {
        super(actor);
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
        if (roomLock != null) {
            throw new IllegalStateException();
        }
        roomLock = new StreamLock(outerActor);
        if (isFull()) {
            roomLock.unblock();
        }
    }

    @Override
    public T getCurrent() {
        return current;
    }

    public void setCurrent(T value) {
        current = value;

    }

    public int size() {
        return  getCurrent() == null? 0: (1+tokens.size());
    }

    public boolean isFull() {
        return  getCurrent() == null? false: tokens.size() == capacity;
    }

    @Override
    public void onSubscribe(Subscription s) {
        int requestNumber;
        synchronized (this) {
            this.subscription = s;
            requestNumber = capacity - size();
        }
        if (requestNumber>0) {
            s.request(requestNumber);
        }
    }

    public void cancel() {
        if (subscription != null) {
            subscription.cancel();
        }
    }

    public void pushBack() {
        pushback = true;
    }

    public synchronized boolean moveNext() {
        boolean res;
        int delta;
        boolean doComplete = false;
        boolean doRuumUnBlock = false;
        Subscription subscriptionLoc;
        synchronized (this) {
            if (pushback) {
                return true;
            }
            if (completed) {
                return false;
            }
            int sizeBefore = size();
            boolean wasFull = isFull();
            setCurrent(tokens.poll());
            res = getCurrent() == null;
            if (res) {
                // no more tokens for now
                if (completionRequested) {
                    completed = true;
                    doComplete = true; // complete is not synchronized, cannot be called from synchronized statement
                } else {
                    block(); // block is synchronized, can be called from synchronized statement
                }
            } else if (roomLock!=null && !completionRequested && wasFull) {
                doRuumUnBlock = true;  // unblock is not synchronized, cannot be called from synchronized statement
            }
            delta = sizeBefore - size();
            subscriptionLoc = subscription;
        }
        if (doRuumUnBlock) {
            roomLock.unblock();
        }
        if (doComplete) {
            complete();
        }
        if (delta > 0 && subscriptionLoc != null) {
            subscriptionLoc.request(delta);
        }
        return res;
    }

    // todo fix
    public boolean hasNext() {
        return getCurrent() != null;
    }

    public synchronized Throwable getCompletionException() {
        return completionException;
    }

    public synchronized boolean isCompleted() {
        return completed;
    }

    @Override
    public synchronized void onNext(T token) {
        if (token == null) {
            throw new NullPointerException();
        }
        boolean doUnblock = false;
        boolean doBlockRoomLock = false;
        synchronized(this) {
            if (completionRequested) {
                return;
            }
            if (getCurrent() == null) {
                setCurrent(token);
                doUnblock = true;
            } else {
                tokens.add(token);
                if (roomLock!=null && isFull()) {
                    doBlockRoomLock = true;
                }
            }
        }
        if (doUnblock) {
            unblock();
        }
        if (doBlockRoomLock) {
            roomLock.block();
        }
    }

    public void complete(Throwable completionException) {
        synchronized(this) {
            if (completionRequested) {
                return;
            }
            completionRequested = true;
            this.completionException = completionException;
            if (getCurrent() != null) {
                return;
            }
            completed = true;
        }
        complete();
    }

    @Override
    public void onError(Throwable throwable) {
        if (throwable != null) {
            throw  new IllegalArgumentException();
        }
        complete(throwable);
    }

    public void onComplete() {
        complete(null);
    }

}
