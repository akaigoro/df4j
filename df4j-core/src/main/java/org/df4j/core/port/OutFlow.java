package org.df4j.core.port;

import org.df4j.core.actor.AsyncProc;
import org.df4j.core.util.linked.LinkImpl;
import org.df4j.core.util.linked.LinkedQueue;
import org.df4j.protocol.Flow;
import org.df4j.protocol.OutMessagePort;
import org.reactivestreams.Subscription;
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
    private LinkedQueue<SubscriptionImpl> activeSubscribtions = new LinkedQueue<>();
    private LinkedQueue<SubscriptionImpl> passiveSubscribtions = new LinkedQueue<>();

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
        SubscriptionImpl subscription = new SubscriptionImpl(subscriber);
        synchronized(transition) {
            if (passiveSubscribtions != null) {
                passiveSubscribtions.add(subscription);
            }
        }
        subscriber.onSubscribe(subscription);
        synchronized(transition) {
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
        synchronized(transition) {
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
        SubscriptionImpl sub;
        synchronized(transition) {
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
        transition.notifyAll();
    }

    private void completAllSubscriptions() {
        for (;;) {
            SubscriptionImpl sub = activeSubscribtions.poll();
            if (sub == null) {
                break;
            }
            sub.onComplete();
        }
        for (;;) {
            SubscriptionImpl sub = passiveSubscribtions.poll();
            if (sub == null) {
                break;
            }
            sub.onComplete();
        }
    }

    public void _onComplete(Throwable cause) {
        synchronized(transition) {
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
        synchronized(transition) {
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
        synchronized(transition) {
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
                transition.wait(millis);
                millis = targetTime - System.currentTimeMillis();
            }
        }
    }

    public T take() throws InterruptedException {
        synchronized(transition) {
            for (;;) {
                T res = tokens.poll();
                if (res != null) {
                    unblock();
                    return res;
                }
                if (completed) {
                    throw new CompletionException(completionException);
                }
                transition.wait();
            }
        }
    }

    public int size() {
        return tokens.size();
    }

    protected class
    SubscriptionImpl extends LinkImpl implements Subscription {
        protected final Subscriber subscriber;
        private long remainedRequests = 0;
        private boolean cancelled = false;

        SubscriptionImpl(Subscriber subscriber) {
            this.subscriber = subscriber;
        }

        public boolean isCancelled() {
            synchronized(transition) {
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
            synchronized(transition) {
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
            synchronized(transition) {
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
            synchronized(transition) {
                remainedRequests--;
                return remainedRequests > 0;
            }
        }

        /**
         * must be unlinked
         * @param cause error
         */
        private void onComplete() {
            synchronized(transition) {
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
