package org.df4j.core.port;

import org.df4j.core.dataflow.BasicBlock;
import org.df4j.protocol.Flow;

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
    Flow.Subscription subscription;

    /**
     * @param parent {@link BasicBlock} to which this port belongs
     */
    public InpFlow(BasicBlock parent) {
        parent.super(false);
    }

    /**
     * creates a port which is subscribed to the {@code #publisher}
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
            return completed && value==null;
        } finally {
            plock.unlock();
        }
    }

    public Throwable getCompletionException() {
        return completionException;
    }

    public boolean isCompletedExceptionslly() {
        return completionException != null;
    }

    public T current() {
        plock.lock();
        try {
            return value;
        } finally {
            plock.unlock();
        }
    }

    public T remove() {
        plock.lock();
        try {
            if (!isReady()) {
                throw new IllegalStateException();
            }
            T res = value;
            value = null;
            block();
            if (subscription == null) {
                return res;
            }
            subscription.request(1);
            return res;
        } finally {
            plock.unlock();
        }
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
}
