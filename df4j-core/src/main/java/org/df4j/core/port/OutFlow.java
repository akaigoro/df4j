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
 *
 * Because of complex logic, it is designed as an Actor itself. However, it still controls firing of the parent actor.
 */

/**
 * is ready when has room to store at least one toke
 * @param <T> type of emitted tokens
 */
public class OutFlow<T> extends CompletablePort implements OutMessagePort<T>, Flow.Publisher<T> {
    protected final int capacity;
    protected ArrayDeque<T> tokens;
    private LinkedQueue<FlowSubscriptionImpl> activeSubscribtions = new LinkedQueue<>();
    private LinkedQueue<FlowSubscriptionImpl> passiveSubscribtions = new LinkedQueue<>();

    public OutFlow(AsyncProc parent, int capacity) {
        super(parent, true);
        this.capacity = capacity;
        tokens = new ArrayDeque<>(capacity);
    }

    public OutFlow(AsyncProc parent) {
        this(parent, 16);
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
                subscription.onComplete(completionException);
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
                return true;
            }
        }
        transferTokens(token, sub);
        return true;
    }

    public void hasItemsEvent() {
        parent.notifyAll();
    }

    protected void transferTokens(T token, FlowSubscriptionImpl sub) {
        LinkedQueue<FlowSubscriptionImpl> asubs;
        LinkedQueue<FlowSubscriptionImpl> psubs;
        for (;;) {
            boolean subIsActive = sub.onNext(token);
            synchronized(parent) {
                if (!subIsActive) {
                    if (passiveSubscribtions == null) {
                        sub.onComplete(completionException);
                    } else {
                        passiveSubscribtions.add(sub);
                    }
                    sub = null;
                }
                if (tokens.size() == 0 || (sub == null && activeSubscribtions.size() == 0)) {
                    if (!isCompleted()) {
                        if (sub != null) {
                            if (activeSubscribtions == null) {
                                sub.onComplete(completionException);
                            } else {
                                activeSubscribtions.add(sub);
                            }
                        }
                        return;
                    }
                    asubs = this.activeSubscribtions;
                    this.activeSubscribtions = null;
                    psubs = this.passiveSubscribtions;
                    this.passiveSubscribtions = null;
                    break;
                }
                token = tokens.poll();
                if (_remainingCapacity() == 1) {
                    unblock();
                }
                if (sub == null) {
                    sub = activeSubscribtions.poll();
                }
            }
        }
        if (sub != null) {
            sub.onComplete(completionException);
        }
        completAllSubscriptions(asubs, psubs);
    }

    public void completAllSubscriptions(LinkedQueue<FlowSubscriptionImpl> asubs, LinkedQueue<FlowSubscriptionImpl> psubs) {
        if (asubs != null) {
            for (;;) {
                FlowSubscriptionImpl sub = asubs.poll();
                if (sub == null) {
                    break;
                }
                sub.onComplete(completionException);
            }
        }
        if (psubs != null) {
            for (;;) {
                FlowSubscriptionImpl sub = psubs.poll();
                if (sub == null) {
                    break;
                }
                sub.onComplete(completionException);
            }
        }
    }

    public void _onComplete(Throwable cause) {
        LinkedQueue<FlowSubscriptionImpl> asubs;
        LinkedQueue<FlowSubscriptionImpl> psubs;
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
            asubs = this.activeSubscribtions;
            this.activeSubscribtions = null;
            psubs = this.passiveSubscribtions;
            this.passiveSubscribtions = null;
        }
        completAllSubscriptions(asubs, psubs);
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
                synchronized(parent) {
                    if (isCompleted()) {
                        if (completionException == null) {
                            subscriber.onComplete();
                        } else {
                            subscriber.onError(completionException);
                        }
                        return;
                    }
                    // remainedRequests was 0, so this subscription was passive
                    passiveSubscribtions.remove(this);
                    token = OutFlow.this.poll();
                    if (token == null) {
                        activeSubscribtions.add(this);
                        return;
                    }
                }
            }
            transferTokens(token, this);
        }

        @Override
        public void cancel() {
            synchronized(parent) {
                if (cancelled) {
                    return;
                }
                cancelled = true;
                synchronized(parent) {
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
        private void onComplete(Throwable cause) {
            synchronized(parent) {
                if (cancelled) {
                    return;
                }
                cancelled = true;
            }
            if (cause == null) {
                subscriber.onComplete();
            } else {
                subscriber.onError(cause);
            }
        }
    }
}
