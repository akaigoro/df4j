package org.df4j.core.activities;

import org.df4j.core.actor.Actor;
import org.df4j.core.actor.ActorGroup;
import org.df4j.core.port.InpScalar;
import org.df4j.core.util.LoggerFactory;
import org.slf4j.Logger;

public class ScalarSubscriberActor extends Actor {
    protected final Logger logger = LoggerFactory.getLogger(this);
    final int delay;
    PublisherActor[] pubs;
    public final InpScalar<Long> inp = new InpScalar<>(this);
    int pubIndex = 0;
    public int cnt;

    public ScalarSubscriberActor(ActorGroup df, int delay, int count, PublisherActor... pubs) {
        super(df);
        this.delay = delay;
        this.cnt = count;
        this.pubs = pubs;
    }

    @Override
    public void start() {
        super.start();
        nextSubscribe();
    }

    public void nextSubscribe() {
        pubs[pubIndex].subscribe(inp);
        pubIndex = (pubIndex+1)%pubs.length;
    }

    @Override
    protected void runAction() throws Throwable {
        Thread.sleep(delay);
        Long in = inp.remove();
        logger.info("cnt="+cnt+"; received:" + in);
        if (cnt > 0) {
            cnt--;
            nextSubscribe();
        } else {
            complete();
        }
    }
}
