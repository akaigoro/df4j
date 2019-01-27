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
    protected int capacity;

    public ReactiveInput(AsyncProc actor, int capacity) {
        super(actor);
        this.queue = new ArrayDeque<>(capacity);
        this.capacity = capacity;
    }

    public ReactiveInput(AsyncProc actor) {
        this(actor, 8);
    }

    protected int size() {
        return queue.size();
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscription.request(capacity);
    }

    @Override
    public void onNext(T item) {
        post(item);
    }

    @Override
    public void onError(Throwable t) {
        postFailure(t);
    }

    @Override
    public synchronized void post(T token) {
        if (subscription == null) {
            throw new IllegalStateException("not yet subscribed");
        }
        if (queue.size() >= capacity) {
            throw new IllegalStateException("no space for next token");
        }
        super.post(token);
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
