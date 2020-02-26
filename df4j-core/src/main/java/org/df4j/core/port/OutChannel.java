package org.df4j.core.port;

import org.df4j.core.dataflow.AsyncProc;
import org.df4j.protocol.ReverseFlow;

/**
 * An active output parameter
 * Has room for single message.
 * Must subscribe to a consumer of type {@link ReverseFlow.Producer} to send message further and unblock this port.
 * @param <T> type of accepted messages.
 */
public class OutChannel<T> extends CompletablePort implements OutMessagePort<T>, ReverseFlow.Producer<T> {
    private T value;
    protected ReverseFlow.ReverseFlowSubscription subscription;

    /**
     * creates {@link OutChannel} not connected to any consumer
     * @param parent {@link AsyncProc} to which this port belongs
     */
    public OutChannel(AsyncProc parent) {
        super(parent, true);
    }

    @Override
    public void onSubscribe(ReverseFlow.ReverseFlowSubscription subscription) {
        if (!isCompleted()) {
            this.subscription = subscription;
        } else if (completionException == null) {
            subscription.onComplete();
        } else {
            subscription.onError(completionException);
        }
    }

    @Override
    public boolean isCompleted() {
        synchronized(parent) {
            return completed && value == null;
        }
    }

    @Override
    public Throwable getCompletionException() {
        synchronized(parent) {
            return completionException;
        }
    }

    public void onNext(T message) {
        if (message == null) {
            throw new IllegalArgumentException();
        }
        synchronized(parent) {
            if (isCompleted()) {
                return;
            }
            if (value != null) {
                throw new IllegalStateException("overflow");
            }
            value = message;
            block();
            if (subscription == null) {
                return;
            }
        }
        subscription.request(1);
    }

    public void _onComplete(Throwable throwable) {
        ReverseFlow.ReverseFlowSubscription sub;
        synchronized(parent) {
            if (isCompleted()) {
                return;
            }
            super._onComplete(throwable);
            if (subscription == null) return;
            sub = subscription;
            subscription = null;
        }
        if (throwable == null) {
            sub.onComplete();
        } else {
            sub.onError(throwable);
        }
    }

    @Override
    public T remove() {
        synchronized(parent) {
            T res = value;
            value = null;
            unblock();
            return res;
        }
    }


    /**
     * called by {@link ReverseFlow.Consumer} when it is completed and asks to not disturb.
     *
     */
    public void cancel() {
        synchronized(parent) {
            if (subscription == null) {
                return;
            }
            subscription.cancel();
            this.subscription = null;
            unblock();
        }
    }
}
