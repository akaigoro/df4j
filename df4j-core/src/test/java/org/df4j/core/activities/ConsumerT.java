package org.df4j.core.activities;

import org.df4j.core.dataflow.ActivityThread;
import org.df4j.core.communicator.AsyncArrayBlockingQueue;
import org.junit.Assert;

import java.util.concurrent.CompletionException;

public class ConsumerT extends Thread implements ActivityThread {
    AsyncArrayBlockingQueue<Integer> queue;
    final int delay;
    Integer in = null;

    public ConsumerT(AsyncArrayBlockingQueue<Integer> queue, int delay) {
        this.queue = queue;
        this.delay = delay;
    }

    @Override
    public void run() {
        System.out.println(" SubscriberT started");
        Throwable cause;
        for (;;) {
            try {
                Integer in = queue.take();
                System.out.println(" got: " + in);
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
        System.out.println(" completed with: " + cause);
    }
}
