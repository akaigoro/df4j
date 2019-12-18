package org.df4j.core.port;

import org.df4j.core.actor.BasicBlock;
import org.df4j.core.protocol.MessageChannel;

/**
 * An active output parameter
 *
 * @param <T> type of accepted tokens.
 */
public class OutChannel<T> extends BasicBlock.Port implements MessageChannel.Producer<T> {
    protected MessageChannel.Consumer<T> defaultConsumer;
    /** extracted token */
    private T value;
    private Throwable completionException;
    protected volatile boolean completed;

    public OutChannel(BasicBlock parent) {
        parent.super(true);
    }

    public OutChannel(BasicBlock parent, MessageChannel.Consumer<T> defaultConsumer) {
        this(parent);
        this.defaultConsumer = defaultConsumer;
    }

    public Throwable getCompletionException() {
        return completionException;
    }

    public synchronized T current() {
        return value;
    }

    public T remove() {
        T res;
        synchronized(this) {
            res = value;
            value = null;
            if (unblock()) {
                return res;
            }
        }
        decBlocking();
        return res;
    }

    public synchronized boolean isCompleted() {
        return completed;
    }

    public void onNext(T message, MessageChannel.Consumer<T> consumer) {
        if (message == null) {
            throw new IllegalArgumentException();
        }
        if (consumer == null) {
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
        if (consumer != null) {
            consumer.offer(this);
        }
    }

    public void onNext(T message) {
        onNext(message, defaultConsumer);
    }

    public void onComplete() {
        onError(null);
    }

    public void onError(Throwable throwable) {
        synchronized(this) {
            if (isCompleted()) {
                return;
            }
            this.completed = true;
            this.completionException = throwable;
            defaultConsumer.offer(this);
            if (unblock()) return;
        }
        decBlocking();
    }
}
