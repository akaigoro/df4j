package org.df4j.core.port;

import org.df4j.core.actor.BasicBlock;
import org.df4j.core.protocol.MessageChannel;

/**
 * A passive input paramerter,
 */
public class InpChannel<T> extends BasicBlock.Port implements MessageChannel.Consumer<T> {
    protected volatile boolean completed;
    protected volatile T value;
    protected volatile Throwable completionException;
    MessageChannel.Producer<T> client;

    public InpChannel(BasicBlock parent) {
        parent.super(false);
    }

    public synchronized boolean isCompleted() {
        return completed;
    }

    public Throwable getCompletionException() {
        return  completionException;
    }

    @Override
    public void offer(MessageChannel.Producer<T> s) {
        if (s == null) {
            throw new NullPointerException();
        }
        synchronized(this) {
            if (isCompleted()) {
                return;
            }
            if (value == null) {
                completed = s.isCompleted();
                completionException = s.getCompletionException();
                value = s.remove();
                if (unblock()) {
                    return;
                }
            } else if (client != null) {
                throw new IllegalStateException();
            } else {
                client = s;
                return;
            }
        }
        decBlocking();
    }

    @Override
    public synchronized void cancel(MessageChannel.Producer<? super T> s) {
        if (client != s) {
            throw new IllegalArgumentException();
        }
        client = null;
    }

    public synchronized T remove() {
        T res;
        if (!isReady()) {
            throw new IllegalStateException();
        }
        res = value;
        if (client == null) {
            value = null;
            block();
        } else {
            value = client.remove();
            completed = client.isCompleted();
            completionException = client.getCompletionException();
            client = null;
        }
        return res;
    }

    public synchronized T current() {
        return value;
    }
}
