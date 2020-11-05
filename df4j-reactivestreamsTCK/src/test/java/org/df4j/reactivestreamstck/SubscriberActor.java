package org.df4j.reactivestreamstck;

import org.df4j.core.actor.AbstractSubscriber;
import org.df4j.core.actor.Actor;
import org.df4j.core.actor.ActorGroup;
import org.df4j.core.port.InpFlow;
import org.df4j.core.util.LoggerFactory;
import org.slf4j.Logger;
import org.testng.Assert;

public class SubscriberActor extends AbstractSubscriber<Long> {
    protected final Logger logger = LoggerFactory.getLogger(this);
    final int delay;
    Long cnt = null;

    public SubscriberActor(int delay) {
        this.delay = delay;
    }

    @Override
    protected void whenNext(Long in) {
        logger.info(" SubscriberActor: inp = " + in);
        if (this.cnt != null) {
            Assert.assertEquals(this.cnt.intValue() - 1, in.intValue());
        }
        this.cnt = in;
    }
}
