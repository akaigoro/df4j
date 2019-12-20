package org.df4j.core.port;

import org.df4j.core.actor.BasicBlock;

import java.util.concurrent.Flow;

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

    public synchronized boolean isCompleted() {
        return completed;
    }

    public Throwable getCompletionException() {
        return completionException;
    }

    public synchronized T current() {
        if (!isReady() || value == null) {
            throw new IllegalStateException();
        }
        return value;
    }

    public  T remove() {
        T res;
        synchronized(this) {
            if (!isReady() || value == null) {
                throw new IllegalStateException();
            }
            res = value;
            value = null;
            block();
            if (subscription == null) {
                return res;
            }
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
        synchronized(this) {
            if (message == null) {
                throw new IllegalArgumentException();
            }
            if (isCompleted()) {
                return;
            }
            value = message;
            unblock();
        }
    }

    @Override
    public synchronized void onError(Throwable throwable) {
        if (isCompleted()) {
            return;
        }
        this.completed = true;
        this.completionException = throwable;
        subscription = null;
        unblock();
    }

    @Override
    public void onComplete() {
        onError(null);
    }

    public synchronized void unsubscribe() {
        if (subscription != null) {
            subscription.cancel();
        }
        subscription = null;
        value = null;
        completionException = null;
        completed = false;
        block();
    }
}
