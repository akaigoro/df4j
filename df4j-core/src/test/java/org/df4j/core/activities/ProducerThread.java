package org.df4j.core.activities;

import org.df4j.core.connector.AsyncArrayBlockingQueue;
import org.df4j.core.dataflow.ActivityThread;
import org.df4j.core.util.Logger;
import org.df4j.core.util.Utils;

public class ProducerThread extends Thread implements ActivityThread {
    public final Logger logger = new Logger(this);
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
        queue.onComplete();
        logger.info("Producer completed");
    }
}
