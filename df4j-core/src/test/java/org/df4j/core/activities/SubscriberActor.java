package org.df4j.core.activities;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.dataflow.Dataflow;
import org.df4j.core.port.InpFlow;
import org.junit.Assert;

public class SubscriberActor extends Actor {
    final int delay;
    public InpFlow<Integer> inp = new InpFlow<>(this);
    Integer in = null;

    public SubscriberActor(Dataflow parent, int delay) {
        super(parent);
        this.delay = delay;
    }

    public SubscriberActor(int delay) {
        this.delay = delay;
    }

    @Override
    protected void runAction() throws Throwable {
        Thread.sleep(delay);
        if (inp.isCompleted()) {
            Throwable completionException = inp.getCompletionException();
            System.out.println(" SubscriberActor: completed with: " + completionException);
            stop(completionException);
            return;
        }
        Integer in = inp.remove();
        System.out.println(" SubscriberActor: inp = " + in);
        if (this.in != null) {
            Assert.assertEquals(this.in.intValue() - 1, in.intValue());
        }
        this.in = in;
    }
}
