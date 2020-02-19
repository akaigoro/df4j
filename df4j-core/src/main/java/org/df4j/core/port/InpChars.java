package org.df4j.core.port;

import org.df4j.core.dataflow.AsyncProc;
import org.df4j.core.util.CharBuffer;
import org.df4j.protocol.CharFlow;
import org.reactivestreams.Subscription;

import java.util.concurrent.CompletionException;

public class InpChars extends CompletablePort implements CharFlow.CharSubscriber {

    private CharBuffer charBuffer;
    protected Subscription subscription;
    private long requestedCount;

    /**
     * creates a port which is subscribed to the {@code #Flow.Publisher}
     *
     * @param parent   {@link AsyncProc} to wich this port belongs
     * @param capacity required capacity
     */
    public InpChars(AsyncProc parent, int capacity) {
        super(parent);
        if (capacity <= 0) {
            throw new IllegalArgumentException();
        }
        charBuffer = new CharBuffer(capacity);
    }

    public InpChars(AsyncProc parent) {
        this(parent, 16);
    }

    public boolean isCompleted() {
        synchronized (parent) {
            return completed && charBuffer.isEmpty();
        }
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        synchronized (parent) {
            if (this.subscription != null) {
                subscription.cancel(); // this is dictated by the spec.
                return;
            }
            this.subscription = subscription;
            requestedCount = charBuffer.remainingCapacity() - requestedCount;
            if (charBuffer.isEmpty()) {
                block();
            }
        }
        subscription.request(requestedCount);
    }

    /**
     * normally this method is called by Flow.Publisher.
     * But before the port is subscribed, this method can be called directly.
     *
     * @param ch token to store
     * @throws IllegalArgumentException when argument is null
     * @throws IllegalStateException    if no room left to store argument
     */
    @Override
    public void onNext(char ch) {
        synchronized (parent) {
            if (charBuffer.buffIsFull()) {
                throw new IllegalStateException();
            }
            if (isCompleted()) {
                return;
            }
            if (subscription != null) {
                requestedCount--;
            }
            charBuffer.add(ch);
            long toRequest = charBuffer.remainingCapacity() - requestedCount;
            if (toRequest > 0) {
                requestedCount += toRequest;
                subscription.request(toRequest);
            }
            unblock();
        }
    }

    public char current() {
        synchronized (parent) {
            if (!ready) {
                throw new IllegalStateException();
            }
            if (charBuffer.isEmpty()) {
                if (isCompleted()) {
                    throw new CompletionException(completionException);
                }
                throw new IllegalStateException();
            }
            return charBuffer.current();
        }
    }

    public char remove() {
        long n;
        char res;
        synchronized (parent) {
            if (!ready) {
                throw new IllegalStateException();
            }
            if (isCompleted()) {
                throw new CompletionException(completionException);
            }
            if (charBuffer.isEmpty()) {
                throw new IllegalStateException();
            }
            res = charBuffer.remove();
            if (charBuffer.isEmpty() && completed) {
                block();
            }
            if (subscription == null) {
                return res;
            }
            n = charBuffer.remainingCapacity();
            requestedCount += n;
        }
        subscription.request(n);
        return res;
    }

}
