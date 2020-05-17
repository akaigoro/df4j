package org.df4j.core.activities;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.dataflow.Dataflow;
import org.df4j.core.port.InpChannel;
import org.df4j.core.util.Logger;

public class ConsumerActor extends Actor {
    protected final Logger logger = new Logger(this);
    final int delay;
    public InpChannel<Long> inp = new InpChannel<>(this);

    public ConsumerActor(int delay, Dataflow parent) {
        super(parent);
        this.delay = delay;
    }

    public ConsumerActor(int delay) {
        this.delay = delay;
    }

    @Override
    protected void runAction() throws Throwable {
        Thread.sleep(delay);
        if (inp.isCompleted()) {
            logger.info(" completed.");
            complete();
        } else {
            Long in = inp.remove();
            logger.info(" got: "+in);
        }
    }
}
