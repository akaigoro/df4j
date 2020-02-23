package org.df4j.core.activities;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.dataflow.Dataflow;
import org.df4j.core.port.OutFlow;
import org.df4j.core.util.Logger;

import java.util.logging.Level;

public class PublisherActor extends Actor {
    protected final Logger logger = new Logger(this);
    public OutFlow<Long> out;
    public long cnt;
    final int delay;
    {
        setLogLevel(Level.OFF);
    }

    public void setLogLevel(Level off) {
        logger.setLevel(off);
    }

    public PublisherActor(Dataflow parent, long cnt, int delay, int capacity) {
        super(parent);
        out = new OutFlow<>(this, capacity);
        this.cnt = cnt;
        this.delay = delay;
        logger.info("PublisherActor: cnt = " + cnt);
    }

    public PublisherActor(Dataflow parent, long cnt, int delay) {
        this(parent, cnt, delay, OutFlow.DEFAULT_CAPACITY);
    }

    public PublisherActor(long cnt, int delay) {
        this(new Dataflow(), cnt, delay);
    }

    public PublisherActor(long cnt) {
        this(cnt, 0);
    }

    @Override
    protected void runAction() throws Throwable {
        if (cnt > 0) {
            logger.info("PublisherActor.onNext(" + cnt+")");
            out.onNext(cnt);
            cnt--;
            Thread.sleep(delay);
        } else {
            logger.info("PublisherActor.onComplete");
            out.onComplete();
            onComplete();
        }
    }
}
