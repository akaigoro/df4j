package org.df4j.core.port;

import org.df4j.core.dataflow.AsyncProc;
import org.df4j.protocol.FlowSubscription;
import org.df4j.protocol.ReverseFlow;

/**
 * An active output parameter
 * Has room for single message.
 * Must subscribe to a consumer of type {@link ReverseFlow.Publisher} to send message further and unblock this port.
 * @param <T> type of accepted messages.
 */
public class OutChannel<T> extends AsyncProc.Port implements ReverseFlow.Producer<T> {
    protected boolean completed;
    protected volatile Throwable completionException;
    private T value;
    protected FlowSubscription subscription;

    /**
     * creates {@link OutChannel} not connected to any consumer
     * @param parent {@link AsyncProc} to which this port belongs
     */
    public OutChannel(AsyncProc parent) {
        parent.super(true);
    }

    @Override
    public void onSubscribe(FlowSubscription subscription) {
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
