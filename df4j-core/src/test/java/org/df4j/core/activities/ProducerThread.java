package org.df4j.core.activities;

import org.df4j.core.communicator.AsyncArrayBlockingQueue;
import org.df4j.core.dataflow.ActivityThread;
import org.df4j.core.util.Logger;
import org.df4j.core.util.Utils;

public class ProducerThread extends Thread implements ActivityThread {
    protected final Logger logger = new Logger(this);
    final int delay;
    int cnt;
    AsyncArrayBlockingQueue<Integer> queue;

    public ProducerThread(int cnt, AsyncArrayBlockingQueue<Integer> queue, int delay) {
        this.queue = queue;
        this.delay = delay;
        this.cnt = cnt;
    }

    @Override
    public void run() {
        logger.info("ProducerT started");
        for (;;) {
            logger.info("cnt: "+cnt);
            if (cnt == 0) {
                queue.onComplete();
                return;
            }
            try {
                queue.put(cnt);
                cnt--;
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Utils.sneakyThrow(e);
            }
        }
    }
}
