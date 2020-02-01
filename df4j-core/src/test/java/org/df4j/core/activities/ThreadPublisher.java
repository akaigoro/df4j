package org.df4j.core.activities;

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
    ArrayBlockingQueue<Long> output = new ArrayBlockingQueue<Long>(16);
    long elements;

    public ThreadPublisher(long elements) {
        this.elements = elements;
    }

    @Override
    public void subscribe(Subscriber<? super Long> s) {
        SimpleSubscription subscription = new SimpleSubscription(s);
        subscription.start();
        s.onSubscribe(subscription);
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

    class SimpleSubscription extends Thread implements Subscription {
        LongSemaphore permits = new LongSemaphore(0);
        Subscriber<? super Long> subscriber;
        volatile boolean cancelled = false;

        public SimpleSubscription(Subscriber<? super Long> s) {
            subscriber = s;
        }

        @Override
        public void request(long n) {
            if (n <= 0) {
                subscriber.onError(new IllegalArgumentException());
                return;
            }
            permits.release(n);
        }

        @Override
        public synchronized void cancel() {
            if (cancelled) {
                return;
            }
            cancelled = true;
            this.interrupt();
        }

        @Override
        public void run() {
            try {
                for (;;) {
                    Long res = null;
                    permits.acquire(1);
                    res = output.take();
                    if (res.intValue() == -1) {
                        output.put(-1L); // for other subscribers
                        subscriber.onComplete();
                        break;
                    }
                    subscriber.onNext(res);
                }
            } catch (InterruptedException e) {
                // cancelled
            }
        }
    }
}
