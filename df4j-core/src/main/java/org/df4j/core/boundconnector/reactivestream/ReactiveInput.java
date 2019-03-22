package org.df4j.core.boundconnector.reactivestream;

import org.df4j.core.boundconnector.messagestream.StreamInput;
import org.df4j.core.tasknode.AsyncProc;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A Queue of tokens
 *
 * @param <T> the type of tokens
 */
public class ReactiveInput<T> extends StreamInput<T> implements Subscriber<T> {
    protected Deque<T> queue;
    protected boolean closeRequested = false;
    protected final int capacity;
    protected int requested = 0;

    public ReactiveInput(AsyncProc actor, int capacity) {
        super(actor);
        this.queue = new ArrayDeque<>(capacity);
        this.capacity = capacity;
    }

    public ReactiveInput(AsyncProc actor) {
        this(actor, 4);
    }

    protected int size() {
        return queue.size();
    }

    protected void makeRequest(int delta) {
        requested += delta;
        this.subscription.request(delta);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        makeRequest(capacity);
    }

    @Override
    public void onError(Throwable t) {
        this.onError(t);
    }

    @Override
    public synchronized void onNext(T token) {
        if (subscription == null) {
            throw new IllegalStateException("not yet subscribed");
        }
        if (queue.size() >= capacity) {
            throw new IllegalStateException("no space for next token");
        }
        requested--;
        super.onNext(token);
    }

    @Override
    public boolean moveNext() {
        boolean res = super.moveNext();
        int freeSpace=capacity - super.size();
        int delta = freeSpace - requested;
        if (delta > 0) {
            subscription.request(delta);
        }
        return res;
    }

    @Override
    public synchronized T next() {
        subscription.request(1);
        return super.next();
    }

    public synchronized boolean  isClosed() {
        return closeRequested && (current == null);
    }
}
