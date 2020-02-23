package org.df4j.core.port;

import org.df4j.core.dataflow.AsyncProc;
import org.df4j.core.util.linked.LinkImpl;
import org.df4j.core.util.linked.LinkedQueue;
import org.df4j.protocol.Flow;
import org.df4j.protocol.FlowSubscription;
import org.reactivestreams.Subscriber;

import java.util.ArrayDeque;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

/**
 * A passive source of messages (like a server).
 * Unblocked initially.
 * It has room for single message.
 * Blocked when overflow.
 * Is ready when has room to store at least one toke
 * @param <T> type of emitted tokens
 */
public class OutFlow<T> extends CompletablePort implements OutMessagePort<T>, Flow.Publisher<T> {
    public static final int DEFAULT_CAPACITY = 16;
    protected final int capacity;
    protected ArrayDeque<T> tokens;
    private LinkedQueue<FlowSubscriptionImpl> activeSubscribtions = new LinkedQueue<>();
    private LinkedQueue<FlowSubscriptionImpl> passiveSubscribtions = new LinkedQueue<>();

    public OutFlow(AsyncProc parent, int capacity) {
        super(parent, capacity>0);
        if (capacity < 0) {
            throw new IllegalArgumentException();
        }
        this.capacity = capacity;
        tokens = new ArrayDeque<>(capacity);
    }

    public OutFlow(AsyncProc parent) {
        this(parent, DEFAULT_CAPACITY);
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        FlowSubscriptionImpl subscription = new FlowSubscriptionImpl(subscriber);
        synchronized(parent) {
            if (passiveSubscribtions != null) {
                passiveSubscribtions.add(subscription);
            }
        }
        subscriber.onSubscribe(subscription);
        synchronized(parent) {
            if (isCompleted()) {
                subscription.onComplete();
            }
        }
    }

    /**
     * how many tokens can be stored in the buffer
     * @return 0 if buffer is full
     */
    private int _remainingCapacity() {
        return capacity - tokens.size();
    }

    /**
     *
     * @param token token to insert
     */
    @Override
    public void onNext(T token) {
        if (!offer(token)) {
            throw new IllegalStateException("buffer overflow");
        }
    }

    public boolean isCompleted() {
        synchronized(parent) {
            return completed && tokens.size() == 0;
        }
    }

    /**
     *
     * @param token token to insert
     * @return true if token inserted
     */
    public boolean offer(T token) {
        if (token == null) {
            throw new NullPointerException();
        }
        FlowSubscriptionImpl sub;
        synchronized(parent) {
            if (completed) {
                return false;
            }
            sub = activeSubscribtions.poll();
            if (sub == null) {
                if (_remainingCapacity() == 0) {
                    return false;
                }
                tokens.add(token);
                hasItemsEvent();
                if (_remainingCapacity() == 0) {
                    block();
                }
            } else {
                boolean subIsActive = sub.onNext(token);
                if (!sub.isCancelled()) {
                    if (subIsActive) {
                        activeSubscribtions.add(sub);
                    } else {
                        passiveSubscribtions.add(sub);
                    }
                }
                if (_remainingCapacity() == 0 && activeSubscribtions.isEmpty()) {
                    block();
                }
            }
            return true;
        }
    }

    public void hasItemsEvent() {
        parent.notifyAll();
    }

    private void completAllSubscriptions() {
        for (;;) {
            FlowSubscriptionImpl sub = activeSubscribtions.poll();
            if (sub == null) {
                break;
            }
            sub.onComplete();
        }
        for (;;) {
            FlowSubscriptionImpl sub = passiveSubscribtions.poll();
            if (sub == null) {
                break;
            }
            sub.onComplete();
        }
    }

    public void _onComplete(Throwable cause) {
        synchronized(parent) {
            if (completed) {
                return;
            }
            completed = true;
            completionException = cause;
            hasItemsEvent();
            if (tokens.size() > 0) {
                return;
            }
            completAllSubscriptions();
        }
    }

    public T poll() {
        synchronized(parent) {
            for (;;) {
                T res = tokens.poll();
                if (res != null) {
                    unblock();
                    return res;
                }
                if (completed) {
                    throw new CompletionException(completionException);
                }
                return null;
            }
        }
    }

    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        synchronized(parent) {
            long millis = unit.toMillis(timeout);
            for (;;) {
                T res = tokens.poll();
                if (res != null) {
                    unblock();
                    return res;
                }
                if (completed) {
                    throw new CompletionException(completionException);
                }
                if (millis <= 0) {
                    return null;
                }
                long targetTime = System.currentTimeMillis() + millis;
                parent.wait(millis);
                millis = targetTime - System.currentTimeMillis();
            }
        }
    }

    public T take() throws InterruptedException {
        synchronized(parent) {
            for (;;) {
                T res = tokens.poll();
                if (res != null) {
                    unblock();
                    return res;
                }
                if (completed) {
                    throw new CompletionException(completionException);
                }
                parent.wait();
            }
        }
    }

    public int size() {
        return tokens.size();
    }

    protected class FlowSubscriptionImpl extends LinkImpl implements FlowSubscription {
        protected final Subscriber subscriber;
        private long remainedRequests = 0;
        private boolean cancelled = false;

        FlowSubscriptionImpl(Subscriber subscriber) {
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
            T token;
            synchronized(parent) {
                if (cancelled) {
                    return;
                }
                remainedRequests += n;
                if (remainedRequests > n) {
                    return;
                }
                if (isCompleted()) {
                    this.onComplete();
                    return;
                }
                // remainedRequests was 0, so this subscription was passive
                passiveSubscribtions.remove(this);
                for (;;) {
                    token = OutFlow.this.poll();
                    if (token == null) {
                        activeSubscribtions.add(this);
                        unblock();
                        break;
                    }
                    boolean subIsActive = this.onNext(token);
                    if (!subIsActive) {
                        passiveSubscribtions.add(this);
                        break;
                    }
                    if (isCompleted()) {
                        this.onComplete();
                        break;
                    }
                }
                if (isCompleted()) {
                    completAllSubscriptions();
                }
            }
        }

        @Override
        public void cancel() {
            synchronized(parent) {
                if (cancelled) {
                    return;
                }
                cancelled = true;
                if (remainedRequests > 0) {
                    if (activeSubscribtions != null) {
                        activeSubscribtions.remove(this);
                    }
                } else {
                    if (passiveSubscribtions != null) {
                        passiveSubscribtions.remove(this);
                    }
                }
            }
        }

        /**
         * must be unlinked
         * @param token token to pass
         * @return
         */
        private boolean onNext(T token) {
            subscriber.onNext(token);
            synchronized(parent) {
                remainedRequests--;
                return remainedRequests > 0;
            }
        }

        /**
         * must be unlinked
         * @param cause error
         */
        private void onComplete() {
            synchronized(parent) {
                if (cancelled) {
                    return;
                }
                cancelled = true;
            }
            if (completionException == null) {
                subscriber.onComplete();
            } else {
                subscriber.onError(completionException);
            }
        }
    }
}
