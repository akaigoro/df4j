package org.df4j.core.actor;

import org.df4j.core.ScalarPublisher;
import org.df4j.core.ScalarSubscriber;
import org.df4j.core.actor.StreamInput;
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
        T message = mainInput.current();
        ScalarSubscription subscription = requests.current();
        if (message != null) {
            subscription.onComplete(message);
        } else {
            Throwable completionException = mainInput.getCompletionException(); // always not null
            subscription.onError(completionException);
        }
    }

}
