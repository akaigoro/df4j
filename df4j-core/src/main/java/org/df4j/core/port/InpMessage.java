package org.df4j.core.port;

import org.df4j.core.actor.BasicBlock;
import org.df4j.core.protocol.MessageStream;

/**
 * Token storage with standard Subscriber&lt;T&gt; interface.
 * It has place for only one token.
 *
 * @param <T> type of accepted tokens.
 */
public class InpMessage<T> extends BasicBlock.Port implements MessageStream.Subscriber<T>, MessageProvider<T> {
    MessageStream.Publisher<T> publisher;
    /** extracted token */
    protected T value;
    private Throwable completionException;
    protected volatile boolean completed;

    public InpMessage(BasicBlock parent) {
        parent.super(false);
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
        MessageStream.Publisher<T> pub;
        T res;
        synchronized(this) {
            if (!isReady() || value == null) {
                throw new IllegalStateException();
            }
            res = value;
            value = null;
            block();
            if (publisher == null) {
                return res;
            }
            pub =  publisher;
        }
        pub.subscribe(this);
        return res;
    }

    public void unsubscribe() {
        MessageStream.Publisher<T> pub = publisher;
        synchronized (this) {
            if (publisher == null) {
                return;
            }
            pub = publisher;
            publisher = null;
        }
        pub.unsubscribe(this);
    }

    /**
     * subscribes in repeating mode:
     * after each {@link #remove()}, subscribes again.
     * This mode can be cancelled with call to {@link #unsubscribe()}.
     * @param pub permanent publisher
     */
    public void subscribeTo(MessageStream.Publisher<T> pub) {
        synchronized(this) {
            value = null;
            block();
            publisher = pub;
        }
        pub.subscribe(this);
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
            if (unblock()) return;
        }
        decBlocking();
    }

    @Override
    public void onError(Throwable throwable) {
        synchronized(this) {
            if (isCompleted()) {
                return;
            }
            this.completed = true;
            this.completionException = throwable;
            publisher = null;
            if (unblock()) return;
        }
        decBlocking();
    }
}
