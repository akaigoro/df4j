package org.df4j.core.activities;

import org.df4j.core.communicator.AsyncArrayBlockingQueue;
import org.df4j.core.dataflow.ActivityThread;
import org.df4j.core.util.Logger;
import org.junit.Assert;

import java.util.concurrent.CompletionException;

public class ConsumerThread extends Thread implements ActivityThread {
    protected final Logger logger = new Logger(this);
    AsyncArrayBlockingQueue<Long> queue;
    final int delay;
    Long in = null;

    public ConsumerThread(AsyncArrayBlockingQueue<Long> queue, int delay) {
        this.queue = queue;
        this.delay = delay;
    }

    @Override
    public void run() {
        logger.info(" SubscriberT started");
        Throwable cause;
        for (;;) {
            try {
                Long in = queue.take();
                logger.info(" got: " + in);
                if (this.in != null) {
                    Assert.assertEquals(this.in.intValue() - 1, in.intValue());
                }
                this.in = in;
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                cause = e;
                break;
            } catch (CompletionException e) {
                cause = e.getCause();
                break;
            }
        }
        logger.info(" completed with: " + cause);
    }
}
