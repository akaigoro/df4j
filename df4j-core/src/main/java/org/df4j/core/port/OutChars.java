package org.df4j.core.port;

import org.df4j.core.dataflow.AsyncProc;
import org.df4j.core.util.CharBuffer;
import org.df4j.core.util.linked.LinkImpl;
import org.df4j.core.util.linked.LinkedQueue;
import org.df4j.protocol.CharFlow;
import org.reactivestreams.Subscription;

import java.util.concurrent.CompletionException;

/**
 * A passive source of characters (like a server).
 * Unblocked initially.
 * Blocked when overflow.
 * Is ready when has room to store at least one toke
 */
public class OutChars extends CompletablePort implements CharFlow.CharPublisher {
    protected final int capacity;
    private CharBuffer charBuffer;
    private LinkedQueue<FlowSubscriptionImpl> activeSubscribtions = new LinkedQueue<>();
    private LinkedQueue<FlowSubscriptionImpl> passiveSubscribtions = new LinkedQueue<>();

    public OutChars(AsyncProc parent, int capacity) {
        super(parent, true);
        this.capacity = capacity;
        charBuffer = new CharBuffer(capacity);
    }

    public OutChars(AsyncProc parent) {
        this(parent, 16);
    }

    @Override
    public void subscribe(CharFlow.CharSubscriber subscriber) {
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
     *
     * @param ch character to insert
     */
    public void onNext(char ch) {
        if (!offer(ch)) {
            throw new IllegalStateException("buffer overflow");
        }
    }

    public boolean isCompleted() {
        synchronized(parent) {
            return completed && charBuffer.isEmpty();
        }
    }

    /**
     *
     * @param ch character to insert
     * @return true if the character inserted
     */
    public boolean offer(char ch) {
        FlowSubscriptionImpl sub;
        synchronized(parent) {
            if (completed) {
                return false;
            }
            sub = activeSubscribtions.poll();
            if (sub == null) {
                if (charBuffer.remainingCapacity() == 0) {
                    return false;
                }
                charBuffer.add(ch);
                hasItemsEvent();
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
            if (!charBuffer.isEmpty()) {
                return;
            }
            completAllSubscriptions();
        }
    }

    public char poll() {
        synchronized(parent) {
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
    }

    protected class FlowSubscriptionImpl extends LinkImpl implements Subscription {
        protected final CharFlow.CharSubscriber subscriber;
        private long remainedRequests = 0;
        private boolean cancelled = false;

        FlowSubscriptionImpl(CharFlow.CharSubscriber subscriber) {
            this.subscriber = subscriber;
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
         * @param ch ch to pass
         * @return
         */
        private boolean onNext(char ch) {
            subscriber.onNext(ch);
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
