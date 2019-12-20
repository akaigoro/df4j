package org.df4j.core.communicator;

import org.df4j.protocol.ReverseFlow;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionException;
import org.df4j.protocol.Flow;
import java.util.concurrent.TimeUnit;

/**
 *  An asynchronous analogue of ArrayBlockingQueue
 *
 *  Flow of messages:
 *  {@link ReverseFlow.Subscriber} => [ {@link AsyncArrayQueue} implements {@link ReverseFlow.Publisher),
 *                                                     {@link Flow.Publisher}] => {@link Flow.Subscriber}
 *
 * @param <T> the type of the values passed through this token container
 */
public class AsyncArrayQueue<T> extends AbstractQueue<T> implements BlockingQueue<T>, ReverseFlow.Publisher<T>, Flow.Publisher<T> {
    protected final int capacity;
    protected ArrayDeque<T> tokens;
    protected Queue<ProducerSubscription> producers = new LinkedList<ProducerSubscription>();
    protected Queue<FlowSubscription> subscribers = new LinkedList<FlowSubscription>();
    protected Throwable completionException;
    protected volatile boolean completed;

    public AsyncArrayQueue(int capacity) {
        this.capacity = capacity;
        tokens = new ArrayDeque<>(capacity);
    }

    @Override
    public void subscribe(ReverseFlow.Subscriber<T> producer) {
        ProducerSubscription subscription = new ProducerSubscription(producer);
        producer.onSubscribe(subscription);
    }
/*
    public void onError(Throwable completionException) {
        Queue<FlowSubscription> subs;
        synchronized(this) {
            if (completed) {
                return;
            }
            completed = true;
            this.completionException = completionException;
            notifyAll();
            if (subscribers.isEmpty()) {
                return;
            }
            subs = subscribers;
            subscribers = null;
        }
        for (;;) {
            FlowSubscription sub = subs.poll();
            if (sub == null) {
                break;
            }
            sub.onError(completionException);
        }
    }

    public void onComplete() {
        onError(null);
    }
*/

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        FlowSubscription subscription = new FlowSubscription(subscriber);
        subscriber.onSubscribe(subscription);
    }

    /**
     *
     * @param token token to insert
     * @return true if token inserted
     */
    @Override
    public boolean offer(T token) {
        FlowSubscription sub;
        synchronized (this) {
            if (subscribers.isEmpty()) {
                return tokens.offer(token);
            }
            sub = subscribers.remove();
        }
        sub.onNext(token);
        return true;
    }

    @Override
    public T poll() {
        return null;
    }

    @Override
    public T peek() {
        return null;
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
    @Override
    public boolean offer(T token, long timeout, TimeUnit unit) throws InterruptedException {
        long millis = unit.toMillis(timeout);
        FlowSubscription sub;
        synchronized(this) {
            for (;;) {
                if (completed) {
                    return false;
                }
                if (!subscribers.isEmpty()) {
                    sub = subscribers.remove();
                    break;
                } else if (tokens.offer(token)) {
                    return true;
                }
                if (millis <= 0) {
                    return false;
                }
                long targetTime = System.currentTimeMillis() + millis;
                wait(millis);
                millis = targetTime - System.currentTimeMillis();
            }
        }
        sub.onNext(token);
        return true;
    }

    @Override
    public synchronized void put(T token) throws InterruptedException {
        FlowSubscription sub;
        synchronized(this) {
            for (;;) {
                if (completed) {
                    return;
                }
                if (!subscribers.isEmpty()) {
                    sub = subscribers.remove();
                    break;
                } else if (tokens.offer(token)) {
                    notifyAll();
                    return;
                }
                wait();
            }
        }
        sub.onNext(token);
    }

    @Override
    public synchronized T take() throws InterruptedException {
        for (;;) {
            T res = tokens.poll();
            if (res != null) {
                return res;
            }
            if (completed) {
                throw new CompletionException(completionException);
            }
            wait();
        }
    }

    @Override
    public synchronized T poll(long timeout, TimeUnit unit) throws InterruptedException {
        long millis = unit.toMillis(timeout);
        for (;;) {
            T res = tokens.poll();
            if (res != null) {
                return res;
            }
            if (completed) {
                throw new CompletionException(completionException);
            }
            if (millis <= 0) {
                return null;
            }
            long targetTime = System.currentTimeMillis() + millis;
            wait(millis);
            millis = targetTime - System.currentTimeMillis();
        }
    }

    @Override
    public synchronized int remainingCapacity() {
        return capacity - tokens.size();
    }

    @Override
    public int drainTo(Collection<? super T> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int drainTo(Collection<? super T> c, int maxElements) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<T> iterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return 0;
    }

    public void onComplete() {
        onError(null);
    }

    public synchronized void onError(Throwable cause) {
        if (completed) {
            return;
        }
        completed = true;
        completionException = cause;
        notifyAll();
        for (;;) {
            FlowSubscription sub1 = subscribers.poll();
            if (sub1 == null) {
                break;
            }
            sub1.onError(cause);
        }
    }

    class ProducerSubscription implements Flow.Subscription {
        protected ReverseFlow.Subscriber<T> producer;
        private long remainedRequests = 0;
        private boolean cancelled = false;

        public ProducerSubscription(ReverseFlow.Subscriber<T> producer) {
            this.producer = producer;
        }

        /**
         * @param n number of messages the producer is able to dekiver now
         */
        @Override
        public synchronized void request(long n) {
            if (n <= 0) {
                throw new IllegalArgumentException();
            }
            synchronized (AsyncArrayQueue.this) {
                if (cancelled) {
                    return;
                }
                if (completed) {
                    producer.cancel();
                    return;
                }
                if (remainedRequests > 0) {
                    remainedRequests += n;
                    return;
                }
                if (!producers.isEmpty()) {
                    producers.add(this);
                    return;
                }
                if (producer.isCompleted()) {
                    completed = true;
                    completionException = producer.getCompletionException();
                    AsyncArrayQueue.this.notifyAll();
                    for (;;) {
                        FlowSubscription sub = subscribers.poll();
                        if (sub == null) {
                            return;
                        }
                        sub.onError(completionException);
                    }
                }
                FlowSubscription sub1 = null;
                while (n > 0) {
                    T value = producer.remove();
                    if (sub1 == null) {
                        sub1 = subscribers.poll();
                    }
                    if (sub1 != null) {
                        if (!sub1.onNext(value)) {
                            sub1 = null;
                        }
                    } else if (tokens.size() < capacity) {
                        tokens.add(value);
                    } else {
                        remainedRequests = n;
                        producers.add(this);
                        break;
                    }
                    n--;
                    AsyncArrayQueue.this.notifyAll();
                }
            }
        }

        @Override
        public void cancel() {
            synchronized (AsyncArrayQueue.this) {
                producers.remove(this);
                cancelled = true;
            }
        }
    }

    class FlowSubscription implements Flow.Subscription {
        protected final Flow.Subscriber subscriber;
        private long remainedRequests = 0;
        private boolean cancelled = false;

        FlowSubscription(Flow.Subscriber subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void request(long n) {
            if (n <= 0) {
                throw new IllegalArgumentException();
            }
            synchronized (AsyncArrayQueue.this) {
                if (cancelled) {
                    return;
                }
                if (remainedRequests > 0) {
                    remainedRequests += n;
                    return;
                }
                while (n > 0) {
                    if (tokens.size() == 0) {
                        if (completed) {
                            if (completionException == null) {
                                subscriber.onComplete();
                            } else {
                                subscriber.onError(completionException);
                            }
                        } else {
                            remainedRequests = n;
                            subscribers.add(this);
                        }
                        return;
                    }
                    T value = tokens.remove();
                    subscriber.onNext(value);
                    n--;
                    AsyncArrayQueue.this.notifyAll();
                }
            }
        }

        @Override
        public void cancel() {
            synchronized (AsyncArrayQueue.this) {
                subscribers.remove(this);
                cancelled = true;
            }
        }

        /**
         *
         * @param value token to pass
         * @return true if can accept more tokens
         */
        public boolean onNext(T value) {
            subscriber.onNext(value);
            remainedRequests--;
            return remainedRequests > 0;
        }

        public void onError(Throwable cause) {
            if (cancelled) {
                return;
            }
            cancelled = true;
            if (cause == null) {
                subscriber.onComplete();
            } else {
                subscriber.onError(cause);
            }
        }
    }
}
