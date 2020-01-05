package org.df4j.core.activities;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.dataflow.Dataflow;
import org.df4j.core.port.OutFlow;
import org.df4j.core.util.Logger;

import java.util.logging.Level;

public class PublisherActor extends Actor {
    protected final Logger logger = new Logger(this);
    public OutFlow<Long> out = new OutFlow<>(this);
    long cnt;
    final int delay;
    {logger.setLevel(Level.OFF);}

    public PublisherActor(Dataflow parent, long cnt, int delay) {
        super(parent);
        this.cnt = cnt;
        this.delay = delay;
    }

    public PublisherActor(long cnt, int delay) {
        this.cnt = cnt;
        this.delay = delay;
    }

    public PublisherActor(long cnt) {
        this(cnt, 0);
    }

    @Override
    protected void runAction() throws Throwable {
        logger.info("PublisherActor: cnt = " + cnt);
        if (cnt > 0) {
            out.onNext(cnt);
            cnt--;
            Thread.sleep(delay);
        } else {
            out.onComplete();
            stop();
        }
    }
}
