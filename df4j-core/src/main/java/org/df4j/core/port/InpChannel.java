package org.df4j.core.port;

import org.df4j.core.dataflow.AsyncProc;
import org.df4j.core.util.linked.Link;
import org.df4j.core.util.linked.LinkImpl;
import org.df4j.core.util.linked.LinkedQueue;
import org.df4j.protocol.ReverseFlow;

import java.util.ArrayDeque;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

/**
 * A passive input parameter.
 * Has room for single value.
 */
public class InpChannel<T> extends CompletablePort implements ReverseFlow.Consumer<T>, InpMessagePort<T> {
    protected int capacity;
    private LinkedQueue<ProducerSubscription> activeProducers = new LinkedQueue<>();
    private LinkedQueue<ProducerSubscription> passiveProducers = new LinkedQueue<>();
    protected ArrayDeque<T> tokens;

    /**
     * @param parent {@link AsyncProc} to which this port belongs
     * @param capacity max buffer capacity
     */
    public InpChannel(AsyncProc parent, int capacity) {
        super(parent);
        this.capacity = capacity;
        tokens = new ArrayDeque<>(capacity);
    }

    public InpChannel(AsyncProc parent) {
        this(parent, 8);
    }

    private boolean _tokensFull() {
        return size() == capacity;
    }

    public boolean isCompleted() {
        synchronized(parent) {
            return completed && tokens.size() == 0;
        }
    }

    public Throwable getCompletionException() {
        synchronized(parent) {
            if (isCompleted()) {
                return completionException;
            } else {
                return null;
            }
        }
    }

    @Override
    public void feedFrom(ReverseFlow.Producer<T> producer) {
        ProducerSubscription subscription = new ProducerSubscription(producer);
        synchronized(parent) {
            passiveProducers.add(subscription);
            producer.onSubscribe(subscription);
        }
    }

    public boolean offer(T token) {
        synchronized(parent) {
            if (completed) {
                return false;
            }
            if (_tokensFull()) {
                return false;
            }
            tokens.add(token);
            unblock();
            return true;
        }
    }

    /**
     * @return the value received from a subscriber, or null if no value was received yet or that value has been removed.
     */
    public T current() {
        synchronized(parent) {
            return tokens.peek();
        }
    }

    @Override
    public void block() {
        synchronized(parent) {
            if (completed) {
                return;
            }
            super.block();
        }
    }

    /**
     * removes and returns incoming value if it is present
     * @return the value received from a subscriber, or null if no value has been received yet or that value has been removed.
     */
    public T poll() {
        synchronized(parent) {
            T res;
            if (tokens.isEmpty()) {
                return null;
            }
            res = tokens.poll();
            parent.notifyAll();

            ProducerSubscription client = activeProducers.peek();
            if (client == null) {
                if (tokens.isEmpty() && !completed) {
                    block();
                }
                return res;
            }
            ReverseFlow.Producer<T> subscriber = client.subscriber;
            if (!subscriber.isCompleted()) {
                T token = subscriber.remove();
                if (token == null) {
                    subscriber.onError(new IllegalArgumentException());
                }
                tokens.add(token);
                client.remainedRequests--;
                if (client.remainedRequests == 0) {
                    activeProducers.remove(client);
                    passiveProducers.add(client);
                }
            } else {
                completed = true;
                completionException = subscriber.getCompletionException();
            }
            unblock();
            return res;
        }
    }

    /**
     *  If there are subscribers waiting for tokens,
     *  then the first subscriber is removed from the subscribers queue and is fed with the token,
     *  otherwise, the token is inserted into this queue, waiting up to the
     *  specified wait time if necessary for space to become available.
     *
     * @param token the element to add
     * @param timeout how long to wait before giving up, in units of
     *        {@code unit}
     * @param unit a {@code TimeUnit} determining how to interpret the
     *        {@code timeout} parameter
     * @return {@code true} if successful, or {@code false} if
     *         the specified waiting time elapses before space is available
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean offer(T token, long timeout, TimeUnit unit) throws InterruptedException {
        if (token == null) {
            throw new NullPointerException();
        }
        long millis = unit.toMillis(timeout);
        synchronized(parent) {
            for (;;) {
                if (completed) {
                    return false;
                }
                if (offer(token)) {
                    return true;
                }
                if (millis <= 0) {
                    return false;
                }
                long targetTime = System.currentTimeMillis() + millis;
                if (millis > 0) {
                    wait(millis);
                    millis = targetTime - System.currentTimeMillis();
                }
            }
        }
    }

    public void add(T token) {
        if (token == null) {
            throw new IllegalArgumentException();
        }
        if (!offer(token)) {
            throw new IllegalStateException();
        }
    }

    public void put(T token) throws InterruptedException {
        if (token == null) {
            throw new NullPointerException();
        }
        synchronized(parent) {
            for (;;) {
                if (completed) {
                    throw new IllegalStateException();
                }
                if (offer(token)) {
                    return;
                }
                parent.wait();
            }
        }
    }

    /**
     * @return the value received from a subscriber
     * @throws IllegalStateException if no value has been received yet or that value has been removed.
     */
    public T remove() throws CompletionException {
        synchronized(parent) {
            if (tokens.isEmpty()) {
                if (completed) {
                    throw new CompletionException("Port already completed", completionException);
                } else {
                    throw new IllegalStateException();
                }
            }
            return poll();
        }
    }

    public int size() {
        return tokens.size();
    }

    protected class ProducerSubscription extends LinkImpl implements ReverseFlow.ReverseFlowSubscription<T> {
        protected final ReverseFlow.Producer<T> subscriber;
        private long remainedRequests = 0;
        private boolean cancelled = false;

        ProducerSubscription(ReverseFlow.Producer<T> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public boolean isCancelled() {
            synchronized(parent) {
                return cancelled;
            }
        }

        /**
         *
         * @param n the increment of demand
         */
        @Override
        public void request(long n) {
            if (n <= 0) {
                subscriber.onError(new IllegalArgumentException());
                return;
            }
            synchronized(parent) {
                if (cancelled) {
                    return;
                }
                if (completed) {
                    return;
                }
                boolean wasActive = remainedRequests > 0;
                remainedRequests += n;
                if (wasActive) {
                    return; //  is active already
                }
                if (transfer()) return;
                if (remainedRequests > 0) {
                    passiveProducers.remove(this);
                    activeProducers.add(this);
                }
                unblock();
            }
        }

        private boolean transfer() {
            while (!_tokensFull() && remainedRequests > 0) {
                if (subscriber.isCompleted()) {
                    _onComplete(subscriber.getCompletionException());
                } else {
                    T token = subscriber.remove();
                    if (token == null) {
                        // wrong subscriber
                        subscriber.onError(new IllegalArgumentException());
                        _cancel();
                        return true;
                    }
                    tokens.add(token);
                    remainedRequests--;
                }
            }
            return false;
        }

        @Override
        public boolean offer(T token) {
            return InpChannel.this.offer(token);
        }

        private void _onComplete(Throwable throwable) {
            synchronized(parent) {
                if (cancelled) {
                    return;
                }
                cancelled = true;
                InpChannel.this._onComplete(throwable);
            }
        }

        @Override
        public void onComplete() {
            _onComplete(null);
        }

        @Override
        public void onError(Throwable throwable) {
            _onComplete(throwable);
        }

        @Override
        public void cancel() {
            synchronized(parent) {
                _cancel();
            }
        }

        private boolean _cancel() {
            if (cancelled) {
                return true;
            }
            cancelled = true;
            if (completed) {
                return true;
            }
            if (remainedRequests > 0) {
                activeProducers.remove(this);
            } else {
                passiveProducers.remove(this);
            }
            return false;
        }
    }
}
