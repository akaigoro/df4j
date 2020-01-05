package org.df4j.core.activities;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.dataflow.Dataflow;
import org.df4j.core.port.InpFlow;
import org.df4j.core.util.Logger;
import org.junit.Assert;

public class SubscriberActor extends Actor {
    protected final Logger logger = new Logger(this);
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
            logger.info(" SubscriberActor: completed with: " + completionException);
            stop(completionException);
            return;
        }
        Integer in = inp.removeAndRequest();
        logger.info(" SubscriberActor: inp = " + in);
        if (this.in != null) {
            Assert.assertEquals(this.in.intValue() - 1, in.intValue());
        }
        this.in = in;
    }
}
