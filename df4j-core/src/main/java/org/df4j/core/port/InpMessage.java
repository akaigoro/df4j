package org.df4j.core.port;

import org.df4j.core.actor.BasicBlock;

import org.df4j.protocol.Flow;

/**
 * Token storage with standard Subscriber&lt;T&gt; interface.
 * It has place for only one token.
 *
 * @param <T> type of accepted tokens.
 */
public class InpMessage<T> extends BasicBlock.Port implements Flow.Subscriber<T>, MessageProvider<T> {
    /** extracted token */
    protected T value;
    private Throwable completionException;
    protected volatile boolean completed;
    Flow.Subscription subscription;

    public InpMessage(BasicBlock parent) {
        parent.super(false);
    }

    public InpMessage(BasicBlock parent, Flow.Publisher<T> publisher) {
        this(parent);
        publisher.subscribe(this);
    }

    public boolean isCompleted() {
        plock.lock();
        try {
            return completed;
        } finally {
            plock.unlock();
        }
    }

    public Throwable getCompletionException() {
        return completionException;
    }

    public T current() {
        plock.lock();
        try {
            if (!isReady() || value == null) {
                throw new IllegalStateException();
            }
            return value;
        } finally {
            plock.unlock();
        }
    }

    public  T remove() {
        T res;
        plock.lock();
        try {
            if (!isReady() || value == null) {
                throw new IllegalStateException();
            }
            res = value;
            value = null;
            block();
            if (subscription == null) {
                return res;
            }
        } finally {
            plock.unlock();
        }
        subscription.request(1);
        return res;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        if (!isReady()) {
            subscription.request(1);
        }
    }

    @Override
    public void onNext(T message) {
        plock.lock();
        try {
            if (message == null) {
                throw new IllegalArgumentException();
            }
            if (isCompleted()) {
                return;
            }
            value = message;
            unblock();
        } finally {
            plock.unlock();
        }
    }

    @Override
    public void onError(Throwable throwable) {
        plock.lock();
        try {
            if (isCompleted()) {
                return;
            }
            this.completed = true;
            this.completionException = throwable;
            subscription = null;
            unblock();
        } finally {
            plock.unlock();
        }
    }

    @Override
    public void onComplete() {
        onError(null);
    }

    public void unsubscribe() {
        plock.lock();
        if (subscription != null) {
            subscription.cancel();
        }
        subscription = null;
        value = null;
        completionException = null;
        completed = false;
        block();
        try {
        } finally {
            plock.unlock();
        }
    }
}
