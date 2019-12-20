package org.df4j.core.port;

import org.df4j.core.actor.BasicBlock;
import org.df4j.core.protocol.ReverseFlow;

import java.util.concurrent.Flow;

/**
 * An active output parameter
 *
 * @param <T> type of accepted tokens.
 */
public class OutChannel<T> extends BasicBlock.Port implements ReverseFlow.Subscriber<T> {
    protected boolean completed;
    protected volatile Throwable completionException;
    private T value;
    protected Flow.Subscription subscription;

    public OutChannel(BasicBlock parent) {
        parent.super(true);
    }

    public OutChannel(BasicBlock parent, ReverseFlow.Publisher<T> consumer) {
        this(parent);
        consumer.subscribe(this);
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
    }

    @Override
    public synchronized boolean isCompleted() {
        return completed;
    }

    @Override
    public Throwable getCompletionException() {
        return completionException;
    }

    public void onNext(T message) {
        if (message == null) {
            throw new IllegalArgumentException();
        }
        if (subscription == null) {
            throw new IllegalArgumentException();
        }
        synchronized(this) {
            if (isCompleted()) {
                return;
            }
            if (value != null) {
                throw new IllegalStateException();
            }
            value = message;
            block();
        }
        subscription.request(1);
    }

    public void onError(Throwable cause) {
        synchronized(this) {
            if (isCompleted()) {
                return;
            }
            this.completed = true;
            this.completionException = cause;
            if (subscription == null) return;
        }
        subscription.request(1);
    }

    public void onComplete() {
        onError(null);
    }

    @Override
    public synchronized T remove() {
        T res = value;
        value = null;
        unblock();
        return res;
    }

    @Override
    public synchronized void cancel() {
        if (subscription == null) {
            return;
        }
        subscription.cancel();
        this.subscription = null;
        unblock();
    }
}
