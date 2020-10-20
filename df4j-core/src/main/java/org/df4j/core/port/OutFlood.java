package org.df4j.core.port;

import org.df4j.core.actor.AsyncProc;
import org.df4j.core.util.linked.LinkImpl;
import org.df4j.core.util.linked.LinkedQueue;
import org.df4j.protocol.Flood;
import org.df4j.protocol.SimpleSubscription;

import java.util.ArrayDeque;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

/**
 * A passive source of messages (like a server).
 * Unblocked initially.
 * Blocked when overflow.
 * Is ready when has room to store at least one toke
 * @param <T> type of emitted tokens
 */
public class OutFlood<T> extends CompletablePort implements OutMessagePort<T>, Flood.Publisher<T> {
    public static final int DEFAULT_CAPACITY = 16;
    protected final int capacity;
    protected ArrayDeque<T> tokens;
    private LinkedQueue<SubscriptionImpl> activeSubscribtions = new LinkedQueue<>();

    public OutFlood(AsyncProc parent, int capacity) {
        super(parent, capacity>0);
        if (capacity < 0) {
            throw new IllegalArgumentException();
        }
        this.capacity = capacity;
        tokens = new ArrayDeque<>(capacity);
    }

    public OutFlood(AsyncProc parent) {
        this(parent, DEFAULT_CAPACITY);
    }

    @Override
    public void subscribe(Flood.Subscriber<? super T> subscriber) {
        SubscriptionImpl subscription = new SubscriptionImpl(subscriber);
        synchronized(transition) {
            if (activeSubscribtions != null) {
                activeSubscribtions.add(subscription);
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
                sub.onNext(token);
                if (!sub.isCancelled()) {
                    activeSubscribtions.add(sub);
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

    protected class SubscriptionImpl extends LinkImpl implements SimpleSubscription {
        protected final Flood.Subscriber subscriber;
        private boolean cancelled = false;

        SubscriptionImpl(Flood.Subscriber subscriber) {
            this.subscriber = subscriber;
        }

        public boolean isCancelled() {
            synchronized(transition) {
                return cancelled;
            }
        }

        @Override
        public void cancel() {
            synchronized(transition) {
                if (cancelled) {
                    return;
                }
                cancelled = true;
                if (activeSubscribtions != null) {
                    activeSubscribtions.remove(this);
                }
            }
        }

        /**
         * must be unlinked
         * @param token token to pass
         */
        private void onNext(T token) {
            subscriber.onNext(token);
        }

        /**
         * must be unlinked
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
