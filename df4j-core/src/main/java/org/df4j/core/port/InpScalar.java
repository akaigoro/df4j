package org.df4j.core.port;

import org.df4j.core.dataflow.BasicBlock;
import org.df4j.protocol.SimpleSubscription;
import org.reactivestreams.*;
import org.df4j.protocol.Scalar;

/**
 * Token storage with standard Subscriber&lt;T&gt; interface.
 * It has place for only one message.
 * After the message is received, this port stays ready until{@link #remove()}. method is called.
 *
 * It can connect both to {@link Scalar.Source} and {@link Publisher}.
 * This port is reusable: to reconnect to another scalar or flow publisher, publisher.subscribe is used.
 * To reconnect to the same flow publisher, {@link #request()} is more efficient than {@link Publisher#subscribe(Subscriber)};
 * Meaning of "completed normally" is different: it is completed after each succesfull receit of the next message
 * until next {@link #remove()} or resubscription.
 *
 * @param <T> type of accepted messages.
 *
 *  TODO clean code for mixed Scalar/Flow subscriptions
 */
public class InpScalar<T> extends BasicBlock.Port implements Scalar.Observer<T> {
    protected T value;
    protected volatile boolean completed = false;
    private Throwable completionException = null;
    private SimpleSubscription simpleSubscription;

    /**
     * @param parent {@link BasicBlock} to which this port belongs
     */
    public InpScalar(BasicBlock parent) {
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
     * like {@link InpFlow#remove()}, does not block, only sets current value to null.
     * To block, create another port.
     * @return current message
     */
    public T remove() {
        plock.lock();
        try {
            T value = this.value;
            this.value = null;
            return value;
        } finally {
            plock.unlock();
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
            unblock();
        } finally {
            plock.unlock();
        }
    }
}
