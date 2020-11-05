package org.df4j.core.port;

import org.df4j.core.actor.AsyncProc;
import org.df4j.protocol.SimpleSubscription;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.concurrent.CompletionException;

/**
 * An input port capable to subscribe to multiple Publishers
 */
public class InpMultiFlow<T> extends CompletablePort {//}, InpMessagePort<T> {
    protected int capacity;
    protected ArrayDeque<T> tokens;
    private LinkedList<Receiver> pendingReceivers = new LinkedList<>();

    /**
     * @param parent {@link AsyncProc} to which this port belongs
     * @param capacity max buffer capacity
     */
    public InpMultiFlow(AsyncProc parent, int capacity) {
        super(parent);
        this.capacity = capacity;
        tokens = new ArrayDeque<>(capacity);
    }

    public InpMultiFlow(AsyncProc parent) {
        this(parent, 8);
    }

    private boolean _tokensFull() {
        return size() == capacity;
    }

    public boolean isCompleted() {
        return completed && tokens.size() == 0;
    }

    public Throwable getCompletionException() {
        if (isCompleted()) {
            return completionException;
        } else {
            return null;
        }
    }

    public SimpleSubscription subscribeTo(Publisher<T> publisher) {
        Receiver receiver = new Receiver();
        publisher.subscribe(receiver);
        return receiver;
    }

    /**
     * @return the value received from a subscriber, or null if no value was received yet or that value has been removed.
     */
    public synchronized T current() {
        return tokens.peek();
    }

    public void add(T token) {
        if (token == null) {
            throw new IllegalArgumentException();
        }
        if (!offer(token)) {
            throw new IllegalStateException();
        }
    }

    public synchronized boolean offer(T token) {
        if (completed) {
            return false;
        }
        if (_tokensFull()) {
            return false;
        }
        tokens.add(token);
        unblock();
        notifyAll();
        return true;
    }

    public synchronized void put(T token) throws InterruptedException {
        if (token == null) {
            throw new NullPointerException();
        }
        for (;;) {
            if (completed) {
                throw new IllegalStateException();
            }
            if (offer(token)) {
                return;
            }
            wait();
        }
    }

    /**
     * @return the value received from a subscriber
     *         or null if buffer is empty.
     */
    public T poll() throws CompletionException {
        T res;
        Receiver receiver;
        synchronized (this) {
            if (tokens.isEmpty() && completed) {
                throw new CompletionException("Port already completed", completionException);
            }
            res = tokens.poll();
            if (tokens.size() == 0) {
                if (completed) {
                    unblock();
                } else {
                    block();
                }
            }
            if (res == null) {
                return null;
            }
            notifyAll();
            receiver = pendingReceivers.poll();
            if (receiver == null) {
                return res;
            }
        }
        receiver.unLoad();
        return res;
    }

    /**
     * @return the value received from a subscriber
     * @throws IllegalStateException if no value has been received yet or that value has been removed.
     */
    public synchronized T remove() throws CompletionException {
        T res = this.poll();
        if (res == null) {
            throw new IllegalStateException();
        }
        return res;
    }

    public int size() {
        return tokens.size();
    }

    class Receiver implements Subscriber<T>, SimpleSubscription {
        Subscription subscription;
        T token = null;
        boolean completed;
        private Throwable completionException;
        private boolean cancelled;

        @Override
        public void onSubscribe(Subscription s) {
            subscription = s;
            s.request(1);
        }

        @Override
        public void onNext(T t) {
            synchronized (InpMultiFlow.this) {
                if (_tokensFull()) {
                    token = t;
                    pendingReceivers.add(this);
                    return;
                } else {
                    tokens.add(t);
                }
            }
            subscription.request(1);
        }

        @Override
        public synchronized void onError(Throwable t) {
            subscription = null;
            if (token == null) {
                InpMultiFlow.this.onError(t);
            } else {
                completed = true;
                completionException = t;
            }
        }

        @Override
        public synchronized void onComplete() {
            subscription = null;
            if (token == null) {
                InpMultiFlow.this.onComplete();
            } else {
                completed = true;
            }
        }

        public synchronized void unLoad() {
            if (token != null) {
                if (!InpMultiFlow.this.offer(token)) {
                    return;
                }
                token = null;
            }
            if (!completed) {
                if (subscription != null) {
                    subscription.request(1);
                }
            } else if (completionException != null) {
                subscription = null;
                InpMultiFlow.this.onError(completionException);
            }
        }

        @Override
        public synchronized void cancel() {
            if (subscription != null) {
                subscription.cancel();
                subscription = null;
            }
            cancelled = true;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }
    }
}
