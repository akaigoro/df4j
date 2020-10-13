package org.df4j.core.activities;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.dataflow.Dataflow;
import org.df4j.core.port.InpFlow;
import org.df4j.core.util.Logger;
import org.junit.Assert;

import java.util.concurrent.CompletionException;
import java.util.logging.Level;

public class SubscriberActor extends Actor {
    protected final Logger logger = new Logger(this);
    final int delay;
    public final InpFlow<Long> inp = new InpFlow<>(this, 1);
    Long cnt = null;

    public SubscriberActor(Dataflow parent, int delay) {
        super(parent);
        this.delay = delay;
    }

    public void setLogLevel(Level all) {
        logger.setLevel(all);
    }

    public SubscriberActor(int delay) {
        this.delay = delay;
    }

    public SubscriberActor() {
        this(0);
    }

    @Override
    protected void runAction() throws InterruptedException {
        Thread.sleep(delay);
        Long in;
        try {
            in = inp.remove();
        } catch (CompletionException e) {
            Throwable completionException = inp.getCompletionException();
            logger.info(" SubscriberActor: completed with: " + completionException);
            if (completionException == null) {
                complete();
            } else {
                completeExceptionally(completionException);
            }
            return;
        }
        logger.info(" SubscriberActor: inp = " + in);
        if (this.cnt != null) {
            Assert.assertEquals(this.cnt.intValue() - 1, in.intValue());
        }
        this.cnt = in;
    }
}
