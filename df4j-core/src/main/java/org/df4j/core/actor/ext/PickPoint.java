package org.df4j.core.actor.ext;

import org.df4j.core.ScalarPublisher;
import org.df4j.core.ScalarSubscriber;
import org.df4j.core.asyncproc.*;

/**
 *  An asynchronous analogue of BlockingQueue
 *
 * @param <T> the type of the values passed through this token container
 */
public class PickPoint<T> extends Actor1<T> implements ScalarPublisher<T> {

    /** place for demands */
    protected ScalarSubscriptionConnector<T> requests = new ScalarSubscriptionConnector<>(this);

    public PickPoint(int capacity) {
        super(capacity);
    }

    public PickPoint() {
    }

    @Override
    public void subscribe(ScalarSubscriber<? super T> s) {
        requests.subscribe(s);
    }

    @Override
    protected void runAction(T token) {
        ScalarSubscription subscription = requests.current();
        subscription.onComplete(token);
    }

}
