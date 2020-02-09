package org.df4j.core.communicator;

import org.df4j.core.base.OutFlowBase;
import org.df4j.core.util.linked.Link;
import org.df4j.core.util.linked.LinkImpl;
import org.df4j.core.util.linked.LinkedQueue;
import org.df4j.protocol.Flow;
import org.df4j.protocol.FlowSubscription;
import org.df4j.protocol.ReverseFlow;
import org.reactivestreams.Subscriber;

import java.util.Collection;
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
 *  {@link ReverseFlow.Producer} =&gt; {@link AsyncArrayBlockingQueue}  =&gt; {@link Subscriber}
 *
 * @param <T> the type of the values passed through this token container
 */
public class AsyncArrayBlockingQueue<T> extends OutFlowBase<T> implements BlockingQueue<T>,
        /** asyncronous analogue of  {@link BlockingQueue#put(Object)} */
        ReverseFlow.Consumer<T>,
        /** asyncronous analogue of  {@link BlockingQueue#take()} */
        Flow.Publisher<T>
//        OutMessagePort<T>
{
    private LinkedQueue<ReverseFlowSubscriptionImpl> activeProducers = new LinkedQueue<>();
    private LinkedQueue<ReverseFlowSubscriptionImpl> passiveProducers = new LinkedQueue<>();
    private final Condition hasRoom;

    public AsyncArrayBlockingQueue(int capacity) {
        super(new ReentrantLock(), capacity);
        hasRoom = qlock.newCondition();
    }

    @Override
    public void subscribe(ReverseFlow.Producer<T> producer) {
        qlock.lock();
        try {
            ReverseFlowSubscriptionImpl reverseSubscription = new ReverseFlowSubscriptionImpl(producer);
            passiveProducers.add(reverseSubscription);
            producer.onSubscribe(reverseSubscription);
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
        if (token == null) {
            throw new NullPointerException();
        }
        long millis = unit.toMillis(timeout);
        FlowSubscriptionImpl sub;
        qlock.lock();
        try {
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
                hasRoom.await(millis, TimeUnit.MILLISECONDS);
                millis = targetTime - System.currentTimeMillis();
            }
        } finally {
            qlock.unlock();
        }
    }

    @Override
    public void put(T token) throws InterruptedException {
        if (token == null) {
            throw new NullPointerException();
        }
        qlock.lock();
        try {
            for (;;) {
                if (completed) {
                    throw new IllegalStateException();
                }
                if (offer(token, 1, TimeUnit.DAYS)) {
                    return;
                }
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
    protected void _hasRoomEvent() {
        while (hasRoom()) {
            ReverseFlowSubscriptionImpl producer = activeProducers.poll();
            if (producer == null) {
                break;
            }
            producer.giveTokens();
        }

        hasRoom.signalAll();
    }

    protected class ReverseFlowSubscriptionImpl extends LinkImpl<ReverseFlowSubscriptionImpl> implements ReverseFlow.ReverseFlowSubscription {
        private final Lock slock = new ReentrantLock();
        protected final ReverseFlow.Producer<T> subscriber;
        private long remainedRequests = 0;
        private boolean cancelled = false;

        ReverseFlowSubscriptionImpl(ReverseFlow.Producer subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public ReverseFlowSubscriptionImpl getItem() {
            return this;
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
                new IllegalArgumentException();
            }
            slock.lock();
            try {
                if (cancelled) {
                    return;
                }
                remainedRequests += n;
                if (remainedRequests > n) {
                    return;
                }
                qlock.lock();
                try {
                    // remainedRequests was 0, so this subscription was passive
                    if (passiveProducers == null) {
                        return; // port closed;
                    }
                    passiveProducers.remove(this);
                    giveTokens();
                } finally {
                    qlock.unlock();
                }
            } finally {
                slock.unlock();
            }
        }

        public void giveTokens() {
            while (remainedRequests > 0 && hasRoom()) {
                T remove = subscriber.remove();
                add(remove);
                remainedRequests--;
            }
            if (remainedRequests== 0) {
                passiveProducers.add(this);
            } else {
                activeProducers.add(this);
            }
        }

        public Link<ReverseFlowSubscriptionImpl> getNext() {
            return super.getNext();
        }

        private void _onComplete(Throwable throwable) {
            slock.lock();
            try {
                if (cancelled) {
                    return;
                }
                cancelled = true;
                AsyncArrayBlockingQueue.this._onComplete(throwable);
            } finally {
                slock.unlock();
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
            slock.lock();
            try {
                if (cancelled) {
                    return;
                }
                cancelled = true;
                qlock.lock();
                try {
                    if (remainedRequests > 0) {
                        if (activeProducers != null) {
                            activeProducers.remove(this);
                        }
                    } else {
                        if (passiveProducers != null) {
                            passiveProducers.remove(this);
                        }
                    }
                } finally {
                    qlock.unlock();
                }
            } finally {
                slock.unlock();
            }
        }
    }
}
