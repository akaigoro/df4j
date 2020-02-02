package org.df4j.core.communicator;

import org.df4j.protocol.Flow;
import org.df4j.protocol.FlowSubscription;
import org.df4j.protocol.ReverseFlow;
import org.reactivestreams.Subscriber;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *  A {@link BlockingQueue} augmented with asynchronous interfaces to save and extract messages, and also interfaces to pass completion signal as required by  {@link Flow}.
 *
 * <p>
 *  Flow of messages:
 *  {@link ReverseFlow.Subscriber} =&gt; {@link AsyncArrayBlockingQueue}  =&gt; {@link Subscriber}
 *
 * @param <T> the type of the values passed through this token container
 */
public class AsyncArrayBlockingQueue<T> extends AsyncArrayBlockingQueueBase<T> implements BlockingQueue<T>,
        /** asyncronous analogue of  {@link BlockingQueue#put(Object)} */
        ReverseFlow.Consumer<T>,
        /** asyncronous analogue of  {@link BlockingQueue#take()} */
        Flow.Publisher<T>
{
    private final Condition hasRoom = qlock.newCondition();
    protected Queue<ProducerSubscription> producers = new LinkedList<ProducerSubscription>();

    public AsyncArrayBlockingQueue(int capacity) {
        super(capacity);
    }

    @Override
    public void offer(ReverseFlow.Producer<T> producer) {
        ProducerSubscription subscription = new ProducerSubscription(producer);
        producer.onSubscribe(subscription);
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

    public void hasRoomEvent() {
        hasRoom.signalAll();
    }

    @Override
    public int drainTo(Collection<? super T> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int drainTo(Collection<? super T> c, int maxElements) {
        throw new UnsupportedOperationException();
    }

    protected class ProducerSubscription implements FlowSubscription {
        private final Lock slock = new ReentrantLock();
        protected ReverseFlow.Producer<T> producer;
        private long remainedRequests = 0;
        private boolean cancelled = false;

        public ProducerSubscription(ReverseFlow.Producer<T> producer) {
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
}
