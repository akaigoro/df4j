package org.df4j.reactivestreamstck;

import org.df4j.core.actor.Actor;
import org.df4j.core.actor.ActorGroup;
import org.df4j.core.port.InpFlow;
import org.df4j.core.util.LoggerFactory;
import org.slf4j.Logger;
import org.testng.Assert;

public class SubscriberActor extends Actor {
    protected final Logger logger = LoggerFactory.getLogger(this);
    final int delay;
    public final InpFlow<Long> inp = new InpFlow<>(this, 1);
    Long cnt = null;

    public SubscriberActor(ActorGroup parent, int delay) {
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
            if (completionException == null) {
                complete();
            } else {
                completeExceptionally(completionException);
            }
            return;
        }
        Long in = inp.remove();
        logger.info(" SubscriberActor: inp = " + in);
        if (this.cnt != null) {
            Assert.assertEquals(this.cnt.intValue() - 1, in.intValue());
        }
        this.cnt = in;
    }
}
