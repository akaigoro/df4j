package org.df4j.core.connector;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.dataflow.Dataflow;
import org.df4j.core.port.InpFlow;
import org.df4j.core.util.linked.Link;
import org.df4j.core.util.linked.LinkImpl;
import org.df4j.core.util.linked.LinkedQueue;
import org.df4j.protocol.Flow;
import org.reactivestreams.Subscription;
import org.reactivestreams.Subscriber;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletionException;

/**
 * Analogue of {@link ArrayBlockingQueue} augmented with asynchronous interface on output end
 * Also, analogue of {@link java.util.concurrent.CompletableFuture} but for a stream of items
 * Also, analogye of SubmissionPublisher (JDK9)
 * @param <T> type of emitted tokens
 */
public class AsyncArrayBlockingQueue<T> extends Actor 
        implements Flow.Subscriber<T>, Flow.Publisher<T>
{
    public static final int DEFAULT_CAPACITY = 16;
    protected final int capacity;
    /** input */
    protected InpFlow<T> input;
    /** demands */
    private LinkedQueuePort<FlowSubscriptionImpl> activeSubscriptions = new LinkedQueuePort<>();
    private LinkedQueue<FlowSubscriptionImpl> passiveSubscriptions = new LinkedQueue<>();

    public AsyncArrayBlockingQueue(Dataflow parent, int capacity) {
        super(parent);
        if (capacity <= 0) {
            throw new IllegalArgumentException();
        }
        this.capacity = capacity;
        input = new InpFlow<T>(this, capacity) {
            @Override
            protected void whenComplete() {
                AsyncArrayBlockingQueue.this._complete(input.getCompletionException());
            }
        };
        start();
    }

    public AsyncArrayBlockingQueue(Dataflow parent) {
        this(parent, DEFAULT_CAPACITY);
    }

    public AsyncArrayBlockingQueue(int capacity) {
        this(new Dataflow(), capacity);
    }

    public AsyncArrayBlockingQueue() {
        this(new Dataflow(), DEFAULT_CAPACITY);
    }

    public synchronized boolean isCompleted() {
        return completed && input.size() == 0;
    }

    public int size() {
        return input.size();
    }

    /**
     * Inserts next token
     * @param token token to insert
     * @return false when input buffer is full
     */
    public boolean offer(T token) {
        return input.offer(token);
    }

    /**
     * Inserts next token
     * blocks when input buffer is full
     * @param token token to insert
     * @throws InterruptedException
     *    when inerrupted
     */
    public void put(T token) throws InterruptedException {
        input.put(token);
    }

    /**
     * Inserts next token
     * @param token token to insert
     * @throws IllegalStateException when input buffer is full
     */
    public void add(T token) {
        input.onNext(token);
    }

    @Override
    public void onSubscribe(org.reactivestreams.Subscription s) {
        input.onSubscribe(s);
    }

    /**
     * Inserts next token
     * @param token token to insert
     * @throws IllegalStateException when input buffer is full
     */
    public void onNext(T token) {
        input.onNext(token);
    }

    public synchronized void onComplete() {
        input.onComplete();
    }

    public void onError(Throwable ex) {
        input.onError(ex);
    }

    /**
     * extracts next token
     * never blocks
     * @return  token
     *       or null when the buffer is empty
     */
    public synchronized T poll() {
        T res = input.poll();
        if (res != null) {
            return res;
        }
        if (completed) {
            throw new CompletionException(completionException);
        }
        return null;
    }

    /**
     * extracts next token
     * @return  token
     * @throws IllegalStateException when the buffer is empty
     */
    public T remove() {
        T result = input.remove();
        return result;
    }

    /**
     * extracts next token
     * blocks when the buffer is empty
     * @return  token
     */
    public T take() throws InterruptedException {
        T res = poll();
        if (res != null) {
            return res;
        }
        FlowSubscriptionImpl subscription = new FlowSubscriptionImpl();
        activeSubscriptions.add(subscription);
        return subscription.get();
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        AsyncFlowSubscriptionImpl subscription = new AsyncFlowSubscriptionImpl(subscriber);
        subscriber.onSubscribe(subscription);
    }

    @Override
    protected void runAction() {
        FlowSubscriptionImpl subscription = activeSubscriptions.poll();
        for (;;) {
            if (subscription == null) {
                break;
            }
            T token = input.poll();
            if (token == null) {
                break;
            }
            subscription.onNext(token);
            subscription = activeSubscriptions.poll();
        }
        if (!input.isCompleted()) {
            return;
        }
        Throwable completionException = input.getCompletionException();
        if (completionException == null) {
            complete();
        } else {
            completeExceptionally(completionException);
        }
        if (subscription != null) {
            subscription.onComplete(completionException);
        }
        for (;;) {
            FlowSubscriptionImpl subscription1 = activeSubscriptions.poll();
            if (subscription1 == null) {
                break;
            }
            subscription1.onComplete(completionException);
        }
        for (;;) {
            FlowSubscriptionImpl subscription1 = passiveSubscriptions.poll();
            if (subscription1 == null) {
                break;
            }
            subscription1.onComplete(completionException);
        }
    }

    protected class FlowSubscriptionImpl extends LinkImpl {
        protected boolean cancelled = false;
        protected T token;

        public synchronized boolean isCancelled() {
            return cancelled;
        }

        public synchronized void cancel() {
            if (cancelled) {
                return;
            }
            cancelled = true;
            this.unlink();
        }

        /**
         * must be unlinked
         * @param token token to pass
         */
        protected synchronized void onNext(T token) {
            this.token = token;
            notifyAll();
        }

        /**
         * must be unlinked
\
         * @param completionException*/
        synchronized void onComplete(Throwable completionException) {
            notifyAll();
        }

        public synchronized T get() throws InterruptedException {
            for (;;) {
                if (token != null) {
                    return token;
                }
                if (isCompleted()) {
                    throw new CompletionException(AsyncArrayBlockingQueue.this.completionException);
                }
                wait();
            }
        }
    }

    protected class AsyncFlowSubscriptionImpl extends FlowSubscriptionImpl
            implements Subscription
    {
        protected final Subscriber subscriber;
        private long remainedRequests = 0;

        AsyncFlowSubscriptionImpl(Subscriber subscriber) {
            this.subscriber = subscriber;
            passiveSubscriptions.add(this);
        }

        private synchronized void park() {
            if (remainedRequests == 0) {
                passiveSubscriptions.add(this); // todo fix
            } else {
                activeSubscriptions.add(this);
            }
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
            // remainedRequests was 0, so this subscription was passive
            passiveSubscriptions.remove(this);
            activeSubscriptions.add(this);
        }

        /**
         * this must be unlinked
         * @param token token to pass
         * @return
         */
        public synchronized void onNext(T token) {
            this.token = token;
            if (token != null) {
                subscriber.onNext(token);
                park();
            } else if (completed) {
                if (completionException == null) {
                    subscriber.onComplete();
                } else {
                    subscriber.onError(completionException);
                }
            }
        }

        /**
         * this must be unlinked
         * @return
         * @param completionException
         */
        public synchronized void onComplete(Throwable completionException) {
            if (completionException == null) {
                subscriber.onComplete();
            } else {
                subscriber.onError(completionException);
            }
        }
    }

    public class LinkedQueuePort<L extends Link> extends LinkedQueue<L> {
        protected final Port port = new Port(AsyncArrayBlockingQueue.this);

        @Override
        public boolean offer(L item) {
            boolean success = super.offer(item);
            if (success) {
                port.unblock();
            }
            return success;
        }

        @Override
        public L poll() {
            L result = super.poll();
            if (size()==0) {
                port.block();
            }
            return result;
        }

        public L remove() {
            L result = super.poll();
            if (result == null) {
                throw new IllegalStateException();
            }
            if (size()==0) {
                port.block();
            }
            return result;
        }

        @Override
        public boolean remove(Link item) {
            boolean result = super.remove(item);
            if (size()==0) {
                port.block();
            }
            return result;
        }
    }
}
