package org.df4j.core.asyncproc;

import org.df4j.core.SubscriptionListener;


/**
 * blocks when there are no active subscribers
 */
public class ScalarSubscriptionBlockingQueue<T> extends ScalarSubscriptionQueue<T> {
    private final Transition.Param outerLock;

    public ScalarSubscriptionBlockingQueue(AsyncProc outerActor) {
        outerLock = outerActor.new Param<ScalarSubscription<T>>(true) {

            @Override
            public  ScalarSubscription<T> next() {
                synchronized (ScalarSubscriptionBlockingQueue.this) {
                    current = poll();
                    if (size() == 0) {
                        block();
                    }
                    return current;
                }
            }
        };
    }

    @Override
    public void subscribe(ScalarSubscriber<? super T> s) {
        super.subscribe(s);
        outerLock.unblock();
    }

    public ScalarSubscription current() {
        return peek();
    }

}
