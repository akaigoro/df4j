package org.df4j.core.activities;

import org.df4j.core.communicator.AsyncArrayBlockingQueue;
import org.df4j.core.communicator.CompletionSubscription;
import org.df4j.core.dataflow.ActivityThread;
import org.df4j.core.util.Logger;
import org.df4j.protocol.Completable;
import org.junit.Assert;

import java.util.LinkedList;
import java.util.concurrent.CompletionException;

public class ConsumerThread extends Thread implements ActivityThread {
    public final Logger logger = new Logger(this);
    AsyncArrayBlockingQueue<Long> queue;
    final int delay;
    Long in = null;
    Throwable cause;

    public ConsumerThread(AsyncArrayBlockingQueue<Long> queue, int delay) {
        this.queue = queue;
        this.delay = delay;
    }

    @Override
    public LinkedList<CompletionSubscription> getSubscriptions() {
        return null;
    }

    @Override
    public Throwable getCompletionException() {
        return cause;
    }

    @Override
    public void run() {
        logger.info(" Consumer thread started");
        try {
            for (;;) {
                logger.info(" Consumer thread: take()");
                Long in = queue.take();
                logger.info(" Consumer thread got: " + in);
                if (this.in != null) {
                    Assert.assertEquals(this.in.intValue() - 1, in.intValue());
                }
                this.in = in;
                Thread.sleep(delay);
            }
        } catch (InterruptedException e) {
            cause = e;
        } catch (CompletionException e) {
            cause = e.getCause();
        }
        logger.info(" Consumer thread completed with: " + cause);
    }

}
