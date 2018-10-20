package org.df4j.core.boundconnector.messagescalar;

import org.df4j.core.boundconnector.messagescalar.ScalarPublisher;
import org.df4j.core.boundconnector.messagescalar.ScalarSubscriber;
import org.df4j.core.boundconnector.messagescalar.SimpleSubscription;
import org.df4j.core.tasknode.AsyncProc;

/**
 * blocks the parent async procedure only when subscribed
 *
 * @param <T> type of token value
 */
public class MultiScalarInput<T> extends AsyncProc.Lock implements ScalarSubscriber<T> {
    SimpleSubscription subscription;
    T value;

    /**
     * not blocked by default
     *
     * @param task parent async procedure
     */
    public MultiScalarInput(AsyncProc task) {
        task.super(false);
    }

    public boolean isDone() {
        return !isBlocked();
    }

    public void subscribeTo(ScalarPublisher publisher) {
        super.turnOff();
        publisher.subscribe(this);
    }

    @Override
    public void onSubscribe(SimpleSubscription subscription) {
        this.subscription = subscription;
    }

    @Override
    public boolean complete(T message) {
        value = message;
        turnOn();
        return true;
    }

    public T get() {
        if (!isDone()) {
            throw new IllegalStateException("value not set yet");
        }
        return value;
    }
}
