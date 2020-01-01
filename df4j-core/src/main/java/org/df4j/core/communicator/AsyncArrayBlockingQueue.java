package org.df4j.core.communicator;

import org.df4j.protocol.Flow;
import org.df4j.protocol.ReverseFlow;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *  A {@link BlockingQueue} augmented with asynchronous interfaces to save and extract messages, and also interfaces to pass completion signal as required by  {@link Flow}.
 *
 * <p>
 *  Flow of messages:
 *  {@link ReverseFlow.Subscriber} =&gt; {@link AsyncArrayBlockingQueue}  =&gt; {@link Flow.Subscriber}
 *
 * @param <T> the type of the values passed through this token container
 */
public class AsyncArrayBlockingQueue<T> extends AbstractQueue<T> implements BlockingQueue<T>,
        /** asyncronous analogue of  {@link BlockingQueue#put(Object)} */
        ReverseFlow.Publisher<T>,
        /** asyncronous analogue of  {@link BlockingQueue#take()} */
        Flow.Publisher<T>
{
    private final Lock qlock = new ReentrantLock();
    private final Condition hasRoom = qlock.newCondition();
    private final Condition hasItems = qlock.newCondition();
    protected final int capacity;
    protected ArrayDeque<T> tokens;
    protected Queue<ProducerSubscription> producers = new LinkedList<ProducerSubscription>();
    protected Queue<FlowSubscriptionImpl> subscribers = new LinkedList<FlowSubscriptionImpl>();
    protected Throwable completionException;
    protected volatile boolean completed;

    public AsyncArrayBlockingQueue(int capacity) {
        this.capacity = capacity;
        tokens = new ArrayDeque<>(capacity);
    }

    @Override
    public void subscribe(ReverseFlow.Subscriber<T> producer) {
        ProducerSubscription subscription = new ProducerSubscription(producer);
        producer.onSubscribe(subscription);
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        FlowSubscriptionImpl subscription = new FlowSubscriptionImpl(subscriber);
        subscriber.onSubscribe(subscription);
    }

    @Override
    public int remainingCapacity() {
        qlock.lock();
        try {
            return capacity - tokens.size();
        } finally {
            qlock.unlock();
        }
    }

    /**
     *
     * @param token token to insert
     * @return true if token inserted
     */
    @Override
    public boolean offer(T token) {
        FlowSubscriptionImpl sub;
        qlock.lock();
        try {
            if (subscribers.isEmpty()) {
                boolean success = tokens.offer(token);
                if (success) {
                    hasItems.signalAll();
                }
                return success;
            }
            sub = subscribers.remove();
        } finally {
            qlock.unlock();
        }
        sub.onNext(token);
        return true;
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
        FlowSubscriptionImpl sub;
        qlock.lock();
        try {
            for (;;) {
                if (completed) {
                    return false;
                }
                if (!subscribers.isEmpty()) {
                    sub = subscribers.remove();
                    break;
                } else if (tokens.offer(token)) {
                    hasItems.signalAll();
                    return true;
                }
                if (millis <= 0) {
                    return false;
                }
                long targetTime = System.currentTimeMillis() + millis;
                hasRoom.await(millis, TimeUnit.MILLISECONDS);
                millis = targetTime - System.currentTimeMillis();
            }
        } finally {
            qlock.unlock();
        }
        sub.onNext(token);
        return true;
    }

    @Override
    public void put(T token) throws InterruptedException {
        FlowSubscriptionImpl sub;
        qlock.lock();
        try {
            for (;;) {
                if (completed) {
                    return;
                }
                if (!subscribers.isEmpty()) {
                    sub = subscribers.remove();
                    break;
                } else if (tokens.offer(token)) {
                    hasItems.signalAll();
                    return;
                }
                hasRoom.await();
            }
        } finally {
            qlock.unlock();
        }
        sub.onNext(token);
    }

    @Override
    public T poll() {
        qlock.lock();
        try {
            for (;;) {
                T res = tokens.poll();
                if (res != null) {
                    hasRoom.signalAll();
                    return res;
                }
                if (completed) {
                    throw new CompletionException(completionException);
                }
                return null;
            }
        } finally {
            qlock.unlock();
        }
    }

    @Override
    public T peek() {
        qlock.lock();
        try {
            for (;;) {
                T res = tokens.peek();
                if (res != null) {
                    return res;
                }
                if (completed) {
                    throw new CompletionException(completionException);
                }
                return null;
            }
        } finally {
            qlock.unlock();
        }
    }

    @Override
    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        qlock.lock();
        try {
            long millis = unit.toMillis(timeout);
            for (;;) {
                T res = tokens.poll();
                if (res != null) {
                    hasRoom.signalAll();
                    return res;
                }
                if (completed) {
                    throw new CompletionException(completionException);
                }
                if (millis <= 0) {
                    return null;
                }
                long targetTime = System.currentTimeMillis() + millis;
                hasItems.await(millis, TimeUnit.MILLISECONDS);
                millis = targetTime - System.currentTimeMillis();
            }
        } finally {
            qlock.unlock();
        }
    }

    @Override
    public T take() throws InterruptedException {
        qlock.lock();
        try {
            for (;;) {
                T res = tokens.poll();
                if (res != null) {
                    hasRoom.signalAll();
                    return res;
                }
                if (completed) {
                    throw new CompletionException(completionException);
                }
                hasItems.await();
            }
        } finally {
            qlock.unlock();
        }
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

    public void onError(Throwable cause) {
        qlock.lock();
        try {
            if (completed) {
                return;
            }
            completed = true;
            completionException = cause;
            hasItems.signalAll();
            for (;;) {
                FlowSubscriptionImpl sub1 = subscribers.poll();
                if (sub1 == null) {
                    break;
                }
                sub1.onError(cause);
            }
        } finally {
            qlock.unlock();
        }
    }

    public void onComplete() {
        onError(null);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    class ProducerSubscription implements ReverseFlow.Subscription {
        private final Lock slock = new ReentrantLock();
        protected ReverseFlow.Subscriber<T> producer;
        private long remainedRequests = 0;
        private boolean cancelled = false;

        public ProducerSubscription(ReverseFlow.Subscriber<T> producer) {
            this.producer = producer;
        }

        @Override
        public boolean isCancelled() {
            slock.lock();
            try {
                return cancelled;
            } finally {
                slock.unlock();
            }
        }

        /**
         * @param n number of messages the producer is able to deliver now
         */
        @Override
        public void request(long n) {
            slock.lock();
            try {
                if (n <= 0) {
                    throw new IllegalArgumentException();
                }
                qlock.lock();
                try {
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
                        hasItems.signalAll();
                        for (;;) {
                            FlowSubscriptionImpl sub = subscribers.poll();
                            if (sub == null) {
                                return;
                            }
                            sub.onError(completionException);
                        }
                    }
                    FlowSubscriptionImpl sub1 = null;
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
                        hasItems.signalAll();
                    }
                } finally {
                    qlock.unlock();
                }
            } finally {
                slock.unlock();
            }
        }

        @Override
        public void cancel() {
            slock.lock();
            try {
                if (cancelled) {
                    return;
                }
                cancelled = true;
                qlock.lock();
                try {
                    producers.remove(this);
                } finally {
                    qlock.unlock();
                }
            } finally {
                slock.unlock();
            }
        }
    }

    class FlowSubscriptionImpl implements Flow.Subscription {
        private final Lock slock = new ReentrantLock();
        protected final Flow.Subscriber subscriber;
        private long remainedRequests = 0;
        private boolean cancelled = false;

        FlowSubscriptionImpl(Flow.Subscriber subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public boolean isCancelled() {
            slock.lock();
            try {
                return cancelled;
            } finally {
                slock.unlock();
            }
        }

        /**
         *
         * @param n the increment of demand
         */
        @Override
        public void request(long n) {
            if (n <= 0) {
                throw new IllegalArgumentException();
            }
            qlock.lock();
            try {
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
                    hasRoom.signalAll();
                    n--;
                }
            } finally {
                qlock.unlock();
            }
        }

        @Override
        public void cancel() {
            slock.lock();
            try {
                if (cancelled) {
                    return;
                }
                cancelled = true;
                qlock.lock();
                try {
                    subscribers.remove(this);
                } finally {
                    qlock.unlock();
                }
            } finally {
                slock.unlock();
            }
        }

        /**
         *
         * @param value token to pass
         * @return true if can accept more tokens
         */
        private boolean onNext(T value) {
            subscriber.onNext(value);
            remainedRequests--;
            return remainedRequests > 0;
        }

        private void onError(Throwable cause) {
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
