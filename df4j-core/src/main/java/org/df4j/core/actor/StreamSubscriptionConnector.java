package org.df4j.core.actor;

import org.df4j.core.asyncproc.*;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * blocks when there are no active subscribers
 */
public class StreamSubscriptionConnector<T> extends StreamSubscriptionQueue<T> implements Publisher<T>
{
    protected final SubscriptionParam parameter;
    /** number of active subscripions in the queue
     * parameter.current is always active or null
     */
    private int activeNumber = 0;

    public StreamSubscriptionConnector(AsyncProc actor) {
        parameter = new SubscriptionParam(actor);
    }

    public StreamSubscription<T> current() {
        return parameter.current();
    }

    public synchronized boolean noActiveSubscribers() {
        return activeNumber == 0;
    }

    @Override
    public void activate(StreamSubscription<T> subscription) {
        synchronized(this) {
            activeNumber++;
            if (parameter.current() == null) {
                parameter.setCurrent(subscription);
            } else {
                super.activate(subscription);
                return;
            }
        }
        parameter.unblock();
    }

    /**
     * when subscriber cancels subscription
     * @param subscription to be cancelled
     * @return true if no active subcriptions remain
     *         false otherwise
     */
    public synchronized void remove(StreamSubscription<T> subscription) {
        super.remove(subscription);
        if (noActiveSubscribers()) {
            parameter.block();
        }
    }

    @Override
    public void subscribe(Subscriber<? super T> s) {
        StreamSubscription<T> subscription = new StreamSubscription<>(this, s);
        synchronized (this) {
            if (parameter.getCurrent() == null) {
                parameter.setCurrent(subscription);
            } else {
                super.add(subscription);
            }
        }
        s.onSubscribe(subscription);
    }

    public void complete(Throwable ex) {
        StreamSubscription<T> current = parameter.getCurrent();
        if (current != null) {
            current.complete(ex);
            parameter.setCurrent(null);
        }
        super.complete(ex);
    }

    public void onComplete() {
        complete(null);
    }

    public void onError(Throwable ex) {
        complete(ex);
    }

    class SubscriptionParam extends Transition.Param<StreamSubscription<T>> {

        public SubscriptionParam(AsyncProc actor) {
            actor.super();
        }

        @Override
        public boolean moveNext() {
            synchronized (StreamSubscriptionConnector.this) {
                if (current != null) {
                    if (!current.isCancelled()) {
                        StreamSubscriptionConnector.this.add(current);
                        if (current.isActive()) {
                            activeNumber++;
                        }
                    }
                    parameter.setCurrent(null);
                }
                if (activeNumber == 0) {
                    block();
                    return true;
                } else {
                    for (;;) {
                        // skip non-ready super
                        current = StreamSubscriptionConnector.this.poll();
                        if (current == null) {
                            block();
                            return true;
                        } else if (current.isCancelled()) {
                            continue; // throw away cancelled
                        } else if (current.isActive()) {
                            activeNumber--;
                            return false;
                        } else {
                            StreamSubscriptionConnector.this.add(current);
                        }
                    }
                }
            }
        }
    }
}
