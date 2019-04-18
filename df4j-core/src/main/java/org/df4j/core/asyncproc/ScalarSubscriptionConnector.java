package org.df4j.core.asyncproc;

import org.df4j.core.ScalarPublisher;
import org.df4j.core.ScalarSubscriber;

/**
 * blocks when there are no active subscribers
 */
public class ScalarSubscriptionConnector<T> extends Transition.Param<ScalarSubscription<T>>
    implements ScalarPublisher<T>
{
    private final ScalarSubscriptionQueue<T> scalarSubscriptionQueue = new ScalarSubscriptionQueue<T>();

    public ScalarSubscriptionConnector(AsyncProc outerActor) {
        outerActor.super(true);
    }

    @Override
    public synchronized boolean moveNext() {
        setCurrent(scalarSubscriptionQueue.poll());
        if ((getCurrent() == null)) {
            block();
            return false;
        }
        return true;
    }

    @Override
    public void subscribe(ScalarSubscriber<? super T> s) {
        synchronized(this) {
            ScalarSubscription subscription = new ScalarSubscription(scalarSubscriptionQueue, s);
            if (getCurrent() == null) {
                setCurrent(subscription);
            } else {
                scalarSubscriptionQueue.subscribe(subscription);
                return;
            }
        }
        unblock();
    }
}
