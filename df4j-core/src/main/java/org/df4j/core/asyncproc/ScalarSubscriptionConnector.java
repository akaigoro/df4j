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
    public  ScalarSubscription next() {
        synchronized (ScalarSubscriptionConnector.this) {
            current = scalarSubscriptionQueue.poll();
            if (scalarSubscriptionQueue.size() == 0) {
                block();
            }
            return current;
        }
    }

    @Override
    public void subscribe(ScalarSubscriber<? super T> s) {
        scalarSubscriptionQueue.subscribe(s);
        unblock();
    }
}
