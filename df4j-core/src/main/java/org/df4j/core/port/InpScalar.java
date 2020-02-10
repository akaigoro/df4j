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
 * To reconnect to the same flow Flow.Publisher, {@link #request()} is more efficient than {@link Flow.Publisher#subscribe(Subscriber)};
 * Meaning of "completed normally" is different: it is completed after each succesfull receit of the next message
 * until next {@link #remove()} or resubscription.
 *
 * @param <T> type of accepted messages.
 *
 *  TODO clean code for mixed Scalar/Flow subscriptions
 */
public class InpScalar<T> extends AsyncProc.Port implements Scalar.Observer<T> {
    protected T value;
    protected volatile boolean completed = false;
    private Throwable completionException = null;
    private SimpleSubscription simpleSubscription;

    /**
     * @param parent {@link AsyncProc} to which this port belongs
     * @param active initial state
     */
    public InpScalar(AsyncProc parent, boolean active) {
        parent.super(false, active);
    }

    public InpScalar(AsyncProc parent) {
        parent.super(false);
    }

    public boolean isCompleted() {
        plock.lock();
        try {
            return completed;
        } finally {
            plock.unlock();
        }
    }

    @Override
    public void onSubscribe(SimpleSubscription subscription) {
        this.simpleSubscription = subscription;
        block();
    }

    public Throwable getCompletionException() {
        return completionException;
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

    private void unsubscribe() {
        if (simpleSubscription != null) {
            simpleSubscription.cancel();
            simpleSubscription = null;
        }
    }

    @Override
    public  void onSuccess(T message) {
        plock.lock();
        try {
            if (completed) {
                return;
            }
            this.completed = true;
            this.value = message;
            unsubscribe();
            unblock();
        } finally {
            plock.unlock();
        }
    }

    @Override
    public  void onError(Throwable throwable) {
        plock.lock();
        try {
            if (completed) {
                return;
            }
            this.completed = true;
            this.completionException = throwable;
            unsubscribe();
            unblock();
        } finally {
            plock.unlock();
        }
    }
}
