package org.df4j.core.activities;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.port.InpFlow;
import org.junit.Assert;

import org.df4j.protocol.Flow;

public class Subscriber extends Actor {
    Flow.Publisher<Integer> pub;
    final int delay;
    InpFlow<Integer> inp = new InpFlow<>(this);
    Integer in = null;

    public Subscriber(Flow.Publisher<Integer> pub, int delay) {
        this.pub = pub;
        this.delay = delay;
        pub.subscribe(inp);
    }

    @Override
    protected void runAction() throws Throwable {
        Thread.sleep(delay);
        if (inp.isCompleted()) {
            Throwable completionException = inp.getCompletionException();
            System.out.println(" completed with: " + completionException);
            stop(completionException);
            return;
        }
        Integer in = inp.remove();
        System.out.println(" got: " + in);
        if (this.in != null) {
            Assert.assertEquals(this.in.intValue() - 1, in.intValue());
        }
        this.in = in;
    }
}
