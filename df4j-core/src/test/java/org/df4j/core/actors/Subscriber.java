package org.df4j.core.actors;

import org.df4j.core.actor.Actor;
import org.df4j.core.port.InpMessage;
import org.junit.Assert;

import java.util.concurrent.Flow;

public class Subscriber extends Actor {
    Flow.Publisher<Integer> pub;
    final int delay;
    InpMessage<Integer> inp = new InpMessage<>(this);
    Integer in = null;

    public Subscriber(Flow.Publisher<Integer> pub, int delay) {
        this.pub = pub;
        this.delay = delay;
        pub.subscribe(inp);
    }

    @Override
    protected void runAction() throws Throwable {
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
        Thread.sleep(delay);
    }
}
