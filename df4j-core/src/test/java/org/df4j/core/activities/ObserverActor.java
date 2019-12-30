package org.df4j.core.activities;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.dataflow.Dataflow;
import org.df4j.core.port.InpFlow;
import org.df4j.core.port.InpScalar;
import org.df4j.protocol.Flow;
import org.junit.Assert;

public class ObserverActor extends Actor {
    Flow.Publisher publisher;
    final int delay;
    public InpScalar<Integer> inp = new InpScalar<>(this);
    Integer in = null;

    public ObserverActor(Dataflow parent, Flow.Publisher publisher, int delay) {
        super(parent);
        this.publisher = publisher;
        this.delay = delay;
        publisher.subscribe(inp);
    }

    public ObserverActor(Dataflow parent, int delay) {
        super(parent);
        this.delay = delay;
    }

    @Override
    protected void runAction() throws Throwable {
        Thread.sleep(delay);
        Integer in = inp.current();
        System.out.println("ObserverActor: inp = " + in);
        if (this.in != null) {
            Assert.assertEquals(this.in.intValue(), in.intValue());
        }
        this.in = in-1;
        inp.reset();
        publisher.subscribe(inp);
    }
}
