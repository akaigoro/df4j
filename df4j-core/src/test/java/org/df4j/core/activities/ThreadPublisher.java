package org.df4j.core.activities;

import org.df4j.core.communicator.AsyncArrayBlockingQueue;
import org.df4j.core.util.Utils;
import org.df4j.protocol.Flow;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.df4j.core.util.LongSemaphore;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;

/**
 * A synchronous implementation of the {@link Flow.Publisher} that can
 * be subscribed to multiple times but each generated token will be received by exactly one subscriber.
 */
public class ThreadPublisher extends Thread implements Flow.Publisher<Long> {
    AsyncArrayBlockingQueue<Long> output = new AsyncArrayBlockingQueue<Long>(16);
    long elements;

    public ThreadPublisher(long elements) {
        this.elements = elements;
    }

    @Override
    public void subscribe(Subscriber<? super Long> s) {
        output.subscribe(s);
    }

    @Override
    public void run() {
        try {
            do {
                elements--;
                output.put(elements);
            } while (elements >= 0);
        } catch (InterruptedException e) {
            Utils.sneakyThrow(e);
        }
    }
}
