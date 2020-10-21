package org.df4j.core.activities;

import org.df4j.core.actor.Actor;
import org.df4j.core.actor.ActorGroup;
import org.df4j.core.port.InpChannel;
import org.df4j.core.util.Logger;

import java.util.concurrent.CompletionException;

public class ConsumerActor extends Actor {
    protected final Logger logger = new Logger(this);
    final int delay;
    public InpChannel<Long> inp = new InpChannel<>(this);

    public ConsumerActor(int delay, ActorGroup parent) {
        super(parent);
        this.delay = delay;
    }

    public ConsumerActor(int delay) {
        this.delay = delay;
    }

    @Override
    protected void runAction() throws InterruptedException {
        Thread.sleep(delay);
        try {
            Long in = inp.remove();
            logger.info(" got: "+in);
        } catch (CompletionException e) {
            logger.info(" completed.");
            complete();
        }
    }
}
