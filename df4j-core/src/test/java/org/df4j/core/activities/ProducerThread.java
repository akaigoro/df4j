package org.df4j.core.activities;

import org.df4j.core.connector.AsyncArrayBlockingQueue;
import org.df4j.core.actor.ActivityThread;
import org.df4j.core.util.LoggerFactory;
import org.df4j.core.util.Utils;
import org.slf4j.Logger;

public class ProducerThread extends ActivityThread {
    public final Logger logger = LoggerFactory.getLogger(this);
    final int delay;
    long cnt;
    AsyncArrayBlockingQueue<Long> queue;

    public ProducerThread(int cnt, AsyncArrayBlockingQueue<Long> queue, int delay) {
        this.queue = queue;
        this.delay = delay;
        this.cnt = cnt;
    }

    @Override
    public void run() {
        logger.info("Producer started");
        while (cnt > 0) {
            logger.info("queue.put( "+cnt+")");
            try {
                queue.put(cnt);
                cnt--;
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Utils.sneakyThrow(e);
            }
        }
        logger.info("queue.onComplete()");
        queue.complete();
        logger.info("Producer completed");
    }
}
