package org.df4j.core.actor;

import org.df4j.core.actor.ext.Actor1;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * receives totalNumber of Longs and cancels the subscription
 */
class ActorSink extends Actor1<Long> {
    Logger parent;
    int maxNumber;
    AtomicInteger received = new AtomicInteger(0);
    String name;

    public ActorSink(Logger parent, int maxNumber, String name) {
        this.parent = parent;
        parent.registerAsyncResult(asyncResult());
        this.name = name;
        if (maxNumber==0) {
            mainInput.onComplete();
            parent.println("  "+ name +": completed 0");
            asyncResult().onComplete();
        } else {
            this.maxNumber = maxNumber;
        }
        start();
    }

    @Override
    protected void runAction(Long val) {
        parent.println("  "+ name +": received "+val);
        received.incrementAndGet();
        if (received.get() >= maxNumber) {
            parent.println("     "+ name +": maxNumber "+maxNumber+" reached");
            mainInput.subscription.cancel();
        }
    }

    @Override
    protected void completion() {
        parent.println("  "+ name +": completed ");
    }

    @Override
    public String toString() {
        return "ActorSink "+name+" isDone:"+result.isDone();
    }
}
