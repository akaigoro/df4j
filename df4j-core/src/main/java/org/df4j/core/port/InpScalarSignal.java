package org.df4j.core.port;

import org.df4j.core.dataflow.BasicBlock;
import org.df4j.protocol.Disposable;
import org.df4j.protocol.Signal;
import org.df4j.protocol.Subscription;

/**
 * Token storage with standard Subscriber&lt;T&gt; interface.
 * It has place for only one token.
 */
public class InpScalarSignal<T> extends BasicBlock.Port implements Signal.Subscriber {
    protected T value;
    protected volatile boolean completed = false;
    protected Subscription subscription;

    public InpScalarSignal(BasicBlock task) {
        task.super(false);
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
    public void onSubscribe(Subscription subscription) {
        plock.lock();
        try {
            this.subscription = subscription;
            subscription.request(1);
        } finally {
            plock.unlock();
        }
    }

    public void unsubscribe() {
        plock.lock();
        try {
            if (subscription == null) {
                return;
            }
            subscription.cancel();
            subscription = null;
        } finally {
            plock.unlock();
        }
    }

    @Override
    public void awake() {
        plock.lock();
        try {
            if (completed) {
                return;
            }
            this.completed = true;
            unblock();
        } finally {
            plock.unlock();
        }
    }
}
