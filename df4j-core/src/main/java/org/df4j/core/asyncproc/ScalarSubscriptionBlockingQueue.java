package org.df4j.core.asyncproc;

import org.df4j.core.SubscriptionListener;


/**
 * blocks when there are no active subscribers
 */
public class ScalarSubscriptionBlockingQueue<T> extends ScalarSubscriptionQueue<T>
        implements SubscriptionListener<T, ScalarSubscription<T>>
{
    private final Transition.Pin outerLock;

    public ScalarSubscriptionBlockingQueue(AsyncProc outerActor) {
        outerLock = outerActor.new Pin(true) {

            protected boolean isParameter() {
                return true;
            }

            @Override
            public Object current() {
                return ScalarSubscriptionBlockingQueue.this.current();
            }

            @Override
            public void purge() {
                ScalarSubscriptionBlockingQueue.this.purge();
            }
        };
    }

    @Override
    public void subscribe(ScalarSubscriber<? super T> s) {
        super.subscribe(s);
        outerLock.unblock();
    }

    public ScalarSubscription<T> current() {
        return peek();
    }

    public void purge() {
        synchronized (this) {
            poll();
            if (size() > 0) {
                return;
            }
        }
        outerLock.block();
    }
}
