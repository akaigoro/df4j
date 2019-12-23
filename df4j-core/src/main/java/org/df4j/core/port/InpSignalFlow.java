package org.df4j.core.port;

import org.df4j.core.dataflow.BasicBlock;
import org.df4j.protocol.Disposable;
import org.df4j.protocol.Signal;
import org.df4j.protocol.Subscription;

public class InpSignalFlow extends BasicBlock.Port implements Signal.Subscriber {
    private Subscription subscription;

    public InpSignalFlow(BasicBlock parent) {
        parent.super(true);
    }

    protected  void acquireFrom(Signal.Publisher publisher) {
        plock.lock();
        try {
            publisher.subscribe(this);
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

    @Override
    public  void awake() {
        plock.lock();
        try {
            unblock();
            subscription.request(1);
        } finally {
            plock.unlock();
        }
    }
}
