package org.df4j.core.activities;

import org.df4j.core.actor.AbstractPublisher;
import org.df4j.core.actor.ActorGroup;
import org.df4j.core.util.LoggerFactory;
import org.slf4j.Logger;

public class PublisherActor extends AbstractPublisher<Long> {
    Logger logger = LoggerFactory.getLogger(this);
    final int delay;
    public long cnt;

    public PublisherActor(ActorGroup parent, long cnt, int delay, int capacity) {
        super(parent, capacity);
        this.cnt = cnt;
        this.delay = delay;
    }

    public PublisherActor(ActorGroup parent, long cnt, int delay) {
        this(parent, cnt, delay, 8);
    }

    public PublisherActor(long cnt, int delay) {
        this(new ActorGroup(), cnt, delay);
    }

    @Override
    protected Long whenNext() throws Throwable {
        Thread.sleep(delay);
        if (cnt == 0) {
            logger.info("sent: completed");
            return null;
        } else {
            if (Math.abs(cnt) < 100 || cnt%10 == 0) {
                logger.info("sent:" + cnt);
            }
            return cnt--;
        }
    }
}
