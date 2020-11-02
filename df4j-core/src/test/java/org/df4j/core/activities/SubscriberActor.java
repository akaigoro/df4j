package org.df4j.core.activities;

import org.df4j.core.actor.AbstractSubscriber;
import org.df4j.core.actor.ActorGroup;
import org.df4j.core.util.LoggerFactory;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;

public class SubscriberActor extends AbstractSubscriber<Long> {
    Logger logger = LoggerFactory.getLogger(this);
    final int delay;

    public SubscriberActor(ActorGroup graph, int delay) {
        this.delay = delay;
    }

    public SubscriberActor(int delay) {
        this(new ActorGroup(), delay);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        super.onSubscribe(subscription);
        logger.info(" onSubscribe");
    }

    @Override
    protected void whenNext(Long item) throws InterruptedException {
        Thread.sleep(delay);
        if (Math.abs(item) < 100 || item%10 == 0) {
            logger.info("  got:"+item);
        }
    }

    @Override
    public void whenComplete(Throwable th) {
        logger.info("  got: completed with "+th);
    }
}
