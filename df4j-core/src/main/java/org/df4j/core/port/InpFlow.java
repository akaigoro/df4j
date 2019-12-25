package org.df4j.core.port;

import org.df4j.core.dataflow.BasicBlock;

import org.df4j.protocol.Flow;
import org.df4j.protocol.Subscription;

/**
 * Token storage with standard Subscriber&lt;T&gt; interface.
 * It has room for only one token.
 *
 * @param <T> type of accepted tokens.
 */
public class InpFlow<T> extends BasicBlock.Port implements Flow.Subscriber<T>, InpMessagePort<T> {
    /** extracted token */
    protected T value;
    private Throwable completionException;
    protected volatile boolean completed;
    Subscription subscription;

    /**
     * @param parent {@link BasicBlock} to which this port belongs
     */
    public InpFlow(BasicBlock parent) {
        parent.super(false);
    }

    /**
     * creates a port which is subscribed to the {@link #publisher}
     * @param parent {@link BasicBlock} to wich this port belongs
     * @param publisher {@link org.df4j.protocol.Flow.Publisher} to subscribe
     */
    public InpFlow(BasicBlock parent, Flow.Publisher<T> publisher) {
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
            return value;
        } finally {
            plock.unlock();
        }
    }

    public  T poll() {
        T res;
        plock.lock();
        try {
            if (!isReady()) {
                return null;
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

    public T remove() {
        plock.lock();
        try {
            if (!isReady()) {
                throw new IllegalStateException();
            }
            return poll();
        } finally {
            plock.unlock();
        }
    }

    @Override
    public void onSubscribe(Subscription subscription) {
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
