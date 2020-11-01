package org.df4j.core.port;

import org.df4j.core.actor.AsyncProc;
import org.df4j.core.util.CharBuffer;
import org.df4j.core.util.LinkedQueue;
import org.df4j.protocol.CharFlow;
import org.reactivestreams.Subscription;

import java.util.concurrent.CompletionException;

/**
 * A passive source of characters (like a server).
 * Unblocked initially.
 * Blocked when overflow.
 * Is ready when has room to store at least one toke
 */
public class OutChars extends CompletablePort implements CharFlow.Publisher {
    protected final int capacity;
    private CharBuffer charBuffer;
    private LinkedQueue<SubscriptionImpl> activeSubscribtions = new LinkedQueue<>();
    private LinkedQueue<SubscriptionImpl> passiveSubscribtions = new LinkedQueue<>();

    public OutChars(AsyncProc parent, int capacity) {
        super(parent, true);
        this.capacity = capacity;
        charBuffer = new CharBuffer(capacity);
    }

    public OutChars(AsyncProc parent) {
        this(parent, 16);
    }

    @Override
    public void subscribe(CharFlow.Subscriber subscriber) {
        SubscriptionImpl subscription = new SubscriptionImpl(subscriber);
        synchronized(this) {
            if (passiveSubscribtions != null) {
                passiveSubscribtions.add(subscription);
            }
        }
        subscriber.onSubscribe(subscription);
        synchronized(this) {
            if (isCompleted()) {
                subscription.onComplete();
            }
        }
    }

    /**
     *
     * @param ch character to insert
     */
    public void onNext(char ch) {
        if (!offer(ch)) {
            throw new IllegalStateException("buffer overflow");
        }
    }

    public synchronized boolean isCompleted() {
        return completed && charBuffer.isEmpty();
    }

    /**
     *
     * @param ch character to insert
     * @return true if the character inserted
     */
    public synchronized boolean offer(char ch) {
        SubscriptionImpl sub;
        if (completed) {
            return false;
        }
        sub = activeSubscribtions.poll();
        if (sub == null) {
            if (charBuffer.remainingCapacity() == 0) {
                return false;
            }
            charBuffer.add(ch);
            notifyAll();
            if (charBuffer.remainingCapacity() == 0) {
                block();
            }
        } else {
            boolean subIsActive = sub.onNext(ch);
            if (subIsActive) {
                activeSubscribtions.add(sub);
            } else {
                passiveSubscribtions.add(sub);
            }
        }
        return true;
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
        if (completed) {
            return;
        }
        completed = true;
        completionException = cause;
        notifyAll();
        if (!charBuffer.isEmpty()) {
            return;
        }
        completAllSubscriptions();
    }

    public synchronized char poll() {
        for (;;) {
            if (!charBuffer.isEmpty()) {
                char res = charBuffer.remove();
                unblock();
                return res;
            }
            if (completed) {
                throw new CompletionException(completionException);
            }
        }
    }

    protected class SubscriptionImpl implements Subscription {
        protected final CharFlow.Subscriber subscriber;
        private long remainedRequests = 0;
        private boolean cancelled = false;

        SubscriptionImpl(CharFlow.Subscriber subscriber) {
            this.subscriber = subscriber;
        }

        /**
         *
         * @param n the increment of demand
         */
        @Override
        public synchronized void request(long n) {
            if (n <= 0) {
                subscriber.onError(new IllegalArgumentException());
                return;
            }
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
                if (charBuffer.isEmpty()) {
                    activeSubscribtions.add(this);
                    break;
                }
                char ch = charBuffer.remove();
                boolean subIsActive = this.onNext(ch);
                if (!subIsActive) {
                    passiveSubscribtions.add(this);
                    break;
                }
            }
            if (isCompleted()) {
                completAllSubscriptions();
            }
        }

        @Override
        public synchronized void cancel() {
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

        /**
         * must be unlinked
         * @param ch ch to pass
         * @return
         */
        private boolean onNext(char ch) {
            subscriber.onNext(ch);
            synchronized(this) {
                remainedRequests--;
                return remainedRequests > 0;
            }
        }

        /**
         * must be unlinked
         * @param cause error
         */
        private synchronized void onComplete() {
            synchronized(this) {
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
