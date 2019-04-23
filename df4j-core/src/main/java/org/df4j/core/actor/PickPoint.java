package org.df4j.core.actor;

import org.df4j.core.ScalarPublisher;
import org.df4j.core.ScalarSubscriber;
import org.df4j.core.SubscriptionCancelledException;
import org.df4j.core.actor.ext.SyncActor;
import org.df4j.core.asyncproc.*;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.NoSuchElementException;

/**
 *  An asynchronous analogue of BlockingQueue
 *
 *  Demultiplexes input stream to separate scalar subscribers.
 *
 * @param <T> the type of the values passed through this token container
 */
public class PickPoint<T> extends SyncActor implements Subscriber<T> , ScalarPublisher<T> {

    protected final StreamInput<T> mainInput;
    /** place for demands */
    protected ScalarSubscriptionConnector<T> requests = new ScalarSubscriptionConnector<>(this);

    public PickPoint(int capacity) {
        this.mainInput = new StreamInput<T>(this, capacity);
        start();
    }

    public PickPoint() {
        this(16);
    }

    @Override
    public void subscribe(ScalarSubscriber<? super T> s) {
        requests.subscribe(s);
    }

    @Override
    public void onSubscribe(Subscription s) {
        mainInput.onSubscribe(s);
    }

    @Override
    public void onNext(T m) {
        mainInput.onNext(m);
    }

    @Override
    public void onError(Throwable ex) {
        mainInput.onError(ex);
    }

    /**
     * processes closing signal
     */
    @Override
    public void onComplete() {
        mainInput.onError(new NoSuchElementException());
    }

    @Override
    protected void runAction() {
        ScalarSubscription subscription = requests.current();
        if (!mainInput.isCompleted()) {
            try {
                subscription.onComplete(mainInput.current());
            } catch (SubscriptionCancelledException e) {
                mainInput.pushBack();
            }
        } else {
            try {
                subscription.onError(mainInput.getCompletionException());
            } catch (SubscriptionCancelledException e) {
            }
        }
    }

    /**
     * blocks when there are no active subscribers
     *
     * This is a dangerous connector.
     * Since any subscription can be cancelled at any time,
     * it may happen that parent actor may discover that this connector is unblocked but empty,
     * and it has to recover in some way.

     */
    public static class ScalarSubscriptionConnector<T> extends StreamParam<ScalarSubscription<T>>
        implements ScalarPublisher<T>
    {
        private final ScalarSubscriptionQueue<T> scalarSubscriptionQueue = new ScalarSubscriptionQueue<T>();

        public ScalarSubscriptionConnector(AsyncProc outerActor) {
            super(outerActor);
        }

        @Override
        public ScalarSubscription<T> getCurrent() {
            return scalarSubscriptionQueue.peek();
        }

        @Override
        public boolean moveNext() {
            synchronized(this) {
                if (scalarSubscriptionQueue.isEmpty()) {
                    return true;
                }
            }
            block();
            return false;
        }

        @Override
        public void subscribe(ScalarSubscriber<? super T> s) {
            synchronized(this) {
                ScalarSubscription<T> subscription = new ScalarSubscription(scalarSubscriptionQueue, s);
                scalarSubscriptionQueue.subscribe(subscription);
            }
            unblock();
        }
    }
}
