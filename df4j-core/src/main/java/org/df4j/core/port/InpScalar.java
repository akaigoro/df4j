package org.df4j.core.port;

import org.df4j.core.dataflow.AsyncProc;
import org.df4j.protocol.Flow;
import org.df4j.protocol.Scalar;
import org.df4j.protocol.SimpleSubscription;
import org.reactivestreams.Subscriber;

/**
 * Token storage with standard Subscriber&lt;T&gt; interface.
 * It has place for only one message.
 * After the message is received, this port stays ready until{@link #remove()}. method is called.
 *
 * It can connect both to {@link Scalar.Source} and {@link Flow.Publisher}.
 * This port is reusable: to reconnect to another scalar or flow Flow.Publisher, Flow.Publisher.subscribe is used.
 * Meaning of "completed normally" is different: it is completed after each succesfull receit of the next message
 * until next {@link #remove()} or resubscription.
 *
 * @param <T> type of accepted messages.
 *
 *  TODO clean code for mixed Scalar/Flow subscriptions
 */
public class InpScalar<T> extends InpCompletable implements Scalar.Observer<T> {
    protected T value;

    /**
     * @param parent {@link AsyncProc} to which this port belongs
     * @param active initial state
     */
    public InpScalar(AsyncProc parent, boolean active) {
        super(parent, active);
    }

    public InpScalar(AsyncProc parent) {
        super(parent);
    }

    @Override
    public void onSubscribe(SimpleSubscription subscription) {
        super.onSubscribe(subscription);
    }

    public T current() {
        plock.lock();
        try {
            return value;
        } finally {
            plock.unlock();
        }
    }

    /**
     * Returns to initial state: empty and blocked.
     * To unblock, make another subscription
     * @return current message
     */
    public T remove() {
        plock.lock();
        try {
            T value = this.value;
            this.value = null;
            this.completed = false;
            return value;
        } finally {
            plock.unlock();
        }
    }

    @Override
    protected void setValue(Object message) {
        value = (T) message;
    }

    @Override
    public  void onSuccess(T message) {
        _onComplete(message, null);
    }

    @Override
    public  void onError(Throwable throwable) {
        _onComplete(null, throwable);
    }
}
