package org.df4j.core.activities;

import org.df4j.core.communicator.AsyncArrayBlockingQueue;
import org.df4j.core.util.Utils;
import org.df4j.protocol.Flow;
import org.reactivestreams.Subscriber;

import java.util.concurrent.TimeUnit;

/**
 * A synchronous implementation of the {@link Flow.Publisher} that can
 * be subscribed to multiple times but each generated token will be received by exactly one subscriber.
 */
public class ThreadPublisher extends Thread implements Flow.Publisher<Long> {
    private final long delay;
    AsyncArrayBlockingQueue<Long> output = new AsyncArrayBlockingQueue<Long>(16);
    long elements;

    public ThreadPublisher(long elements, long delay) {
        this.elements = elements;
        this.delay = delay;
    }

    public ThreadPublisher(long elements) {
        this(elements, 0);
    }

    @Override
    public void subscribe(Subscriber<? super Long> s) {
        output.subscribe(s);
    }

    @Override
    public void run() {
        try {
            for (long k=0; k<elements; k++) {
                Thread.sleep(delay);
                output.put(k);
            }
            output.onComplete();
        } catch (InterruptedException e) {
            Utils.sneakyThrow(e);
        } finally {
            output.onComplete();
        }
    }
}
