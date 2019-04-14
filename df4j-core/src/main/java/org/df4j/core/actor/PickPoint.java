package org.df4j.core.actor;

import org.df4j.core.asyncproc.*;

/**
 *  An asynchronous analogue of BlockingQueue
 *  (only on output end, while from the input side it does not block)
 *
 * @param <T> the type of the values passed through this token container
 */
public class PickPoint<T> extends Actor implements ScalarPublisher<T> {
    protected StreamInput<T> resources = new StreamInput<>(this);
    /** place for demands */
    protected ScalarSubscriptionBlockingQueue<T> requests = new ScalarSubscriptionBlockingQueue<>(this);

    @Override
    public void subscribe(ScalarSubscriber<? super T> s) {
        requests.subscribe(s);
    }

    @Override
    protected void runAction() {
        T token = resources.current();
        ScalarSubscriptionQueue.ScalarSubscription subscription = requests.current();
        subscription.onComplete(token);
    }

    public void onNext(T t) {
        resources.onNext(t);
    }
}
