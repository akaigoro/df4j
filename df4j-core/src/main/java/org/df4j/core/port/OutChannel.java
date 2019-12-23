package org.df4j.core.port;

import org.df4j.core.dataflow.BasicBlock;
import org.df4j.protocol.ReverseFlow;

import org.df4j.protocol.Subscription;

/**
 * An active output parameter
 *
 * @param <T> type of accepted tokens.
 */
public class OutChannel<T> extends BasicBlock.Port implements ReverseFlow.Subscriber<T> {
    protected boolean completed;
    protected volatile Throwable completionException;
    private T value;
    protected Subscription subscription;

    public OutChannel(BasicBlock parent) {
        parent.super(true);
    }

    public OutChannel(BasicBlock parent, ReverseFlow.Publisher<T> consumer) {
        this(parent);
        consumer.subscribe(this);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
    }

    @Override
    public boolean isCompleted() {
        plock.lock();
        try {
            return completed;
        } finally {
            plock.unlock();
        }
    }

    @Override
    public Throwable getCompletionException() {
        plock.lock();
        try {
            return completionException;
        } finally {
            plock.unlock();
        }
    }

    public void onNext(T message) {
        if (message == null) {
            throw new IllegalArgumentException();
        }
        if (subscription == null) {
            throw new IllegalArgumentException();
        }
        plock.lock();
        try {
            if (isCompleted()) {
                return;
            }
            if (value != null) {
                throw new IllegalStateException();
            }
            value = message;
            block();
        } finally {
            plock.unlock();
        }
        subscription.request(1);
    }

    public void onError(Throwable cause) {
        plock.lock();
        try {
            if (isCompleted()) {
                return;
            }
            this.completed = true;
            this.completionException = cause;
            if (subscription == null) return;
        } finally {
            plock.unlock();
        }
        subscription.request(1);
    }

    public void onComplete() {
        onError(null);
    }

    @Override
    public T remove() {
        plock.lock();
        try {
            T res = value;
            value = null;
            unblock();
            return res;
        } finally {
            plock.unlock();
        }
    }

    @Override
    public void cancel() {
        plock.lock();
        try {
            if (subscription == null) {
                return;
            }
            subscription.cancel();
            this.subscription = null;
            unblock();
        } finally {
            plock.unlock();
        }
    }
}
