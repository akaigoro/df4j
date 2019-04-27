package org.df4j.core.actor;

import org.df4j.core.ScalarPublisher;
import org.df4j.core.ScalarSubscriber;
import org.df4j.core.SubscriptionCancelledException;
import org.df4j.core.asyncproc.ScalarSubscription;
import org.df4j.core.asyncproc.ScalarSubscriptionQueue;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 *  An asynchronous analogue of BlockingQueue
 *
 *  Demultiplexes input stream to separate scalar subscribers.
 *
 * @param <T> the type of the values passed through this token container
 */
public class PickPoint<T> implements ScalarPublisher<T> {

    protected int capacity;
    protected Queue<T> tokens;
    /** to monitor existence of the room for additional tokens */
    protected StreamLock roomLock;
    /** extracted token */
    protected boolean completionRequested = false;
    protected boolean completed = false;
    protected T current;
    protected Throwable completionException;

    /** place for demands */
    private final ScalarSubscriptionQueue<T> scalarSubscriptionQueue = new ScalarSubscriptionQueue<T>();

    public PickPoint(int fullCapacity) {
        if (fullCapacity <= 0) {
            throw new IllegalArgumentException();
        }
        this.capacity = fullCapacity;
        this.tokens = new ArrayDeque<>(fullCapacity);
    }

    public PickPoint() {
        this(16);
    }

    @Override
    public void subscribe(ScalarSubscriber<? super T> subscriber) {
        T nextValue = null;
        synchronized(this) {
            if (!completed) {
                nextValue = tokens.poll();
                if (nextValue == null) {
                    ScalarSubscription<T> subscription = new ScalarSubscription(scalarSubscriptionQueue, subscriber);
                    scalarSubscriptionQueue.subscribe(subscription);
                    return;
                }
            }
        }
        if (nextValue != null) {
            subscriber.onComplete(nextValue);
        } else if (completionException == null){
            subscriber.onComplete(null);
        } else {
            subscriber.onError(completionException);
        }
    }

    /**
     * delivers the token to exactly one subscriber,
     * unless the stream is completed.
     *
     * @param token
     */
    public void onNext(T token) {
        if (token == null) {
            throw new NullPointerException();
        }
        for (;;) {
            ScalarSubscription<T> subs;
            synchronized(this) {
                if (completionRequested) {
                    return;
                }
                subs = scalarSubscriptionQueue.poll();
                if (subs == null) {
                    tokens.add(token);
                    return;
                }
            }
            try {
                subs.onComplete(token);
                return;
            } catch (SubscriptionCancelledException e) {
            }
        }
    }

    protected synchronized void completeInput(Throwable throwable) {
        if (throwable != null) {
            throw  new IllegalArgumentException();
        }
        if (completionRequested) {
            return;
        }
        completionRequested = true;
        this.completionException = throwable;
        if (tokens.isEmpty()) {
            completed = true;
        }
    }

    public void onComplete() {
        completeInput(null);
        scalarSubscriptionQueue.onComplete(null);
    }

    public void onError(Throwable throwable) {
        completeInput(throwable);
        scalarSubscriptionQueue.onError(throwable);
    }
}
