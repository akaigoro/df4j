package org.df4j.core.port;

import org.df4j.core.actor.AsyncProc;
import org.df4j.protocol.Flow;
import org.df4j.protocol.Scalar;
import org.df4j.protocol.SimpleSubscription;

import java.util.concurrent.CompletionException;

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
public class InpScalar<T> extends CompletablePort implements Scalar.Observer<T>, InpMessagePort<T> {
    private SimpleSubscription subscription;
    protected T value;

    /**
     * @param parent {@link AsyncProc} to which this port belongs
     *
     */
    public InpScalar(AsyncProc parent) {
        super(parent, false);
    }

    @Override
    public void onSubscribe(SimpleSubscription subscription) {
        if (completed) {
            subscription.cancel();
            return;
        }
        this.subscription = subscription;
        block();
    }

    public void unsubscribe() {
        SimpleSubscription sub;
        synchronized(transition) {
            if (subscription == null) {
                return;
            }
            sub = subscription;
            subscription = null;
        }
        sub.cancel();
    }

    public T current() {
        synchronized(transition) {
            if (!completed) {
                throw new IllegalStateException();
            }
            if (isCompletedExceptionally()) {
                throw new CompletionException(getCompletionException());
            }
            return value;
        }
    }

    @Override
    public T poll() {
        if (!completed) {
            return null;
        }
        if (isCompletedExceptionally()) {
            throw new CompletionException(getCompletionException());
        }
        return value;
    }

    /**
     * Returns to initial state: empty and blocked.
     * To unblock, make another subscription
     * @return current message
     */
    public T remove() {
        synchronized(transition) {
            if (!ready) {
                throw new IllegalStateException();
            }
            T value = this.value;
            this.value = null;
            this.completed = false;
            block();
            return value;
        }
    }

    @Override
    public  void onSuccess(T message) {
        synchronized(transition) {
            if (completed) {
                return;
            }
            this.value = message;
            super.onComplete();
        }
    }
}
