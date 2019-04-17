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
        current = scalarSubscriptionQueue.poll();
        if ((current == null)) {
            block();
            return false;
        }
        return true;
    }

    @Override
    public synchronized void subscribe(ScalarSubscriber<? super T> s) {
        ScalarSubscription subscription = new ScalarSubscription(scalarSubscriptionQueue, s);
        if (current == null) {
            current = subscription;
            unblock();
        } else {
            scalarSubscriptionQueue.subscribe(subscription);
        }
    }
}
