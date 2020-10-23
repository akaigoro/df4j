package org.df4j.core.activities;

import org.df4j.core.actor.Actor;
import org.df4j.core.actor.ActorGroup;
import org.df4j.core.port.OutChannel;
import org.df4j.core.util.LoggerFactory;
import org.slf4j.Logger;

public class ProducerActor extends Actor {
    protected final Logger logger = LoggerFactory.getLogger(this);
    final int delay;
    long cnt;
    public OutChannel<Long> out;

    public ProducerActor(ActorGroup parent, int cnt, int delay) {
        super(parent);
        out = new OutChannel<>(this);
        this.cnt = cnt;
        this.delay = delay;
    }

    public ProducerActor(int cnt, int delay) {
        out = new OutChannel<>(this);
        this.cnt = cnt;
        this.delay = delay;
    }

    public ProducerActor(int cnt) {
        this(cnt, 0);
    }

    @Override
    protected void runAction() throws Throwable {
        logger.info("cnt: "+cnt);
        if (cnt == 0) {
            out.onComplete();
            complete();
        } else {
            out.onNext(cnt);
            cnt--;
            Thread.sleep(delay);
        }
    }
}
