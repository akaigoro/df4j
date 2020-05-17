package org.reactivestreams.example.unicast;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;

/**
 * A synchronous implementation of the {@link Publisher} that can
 * be subscribed to multiple times but each generated token will be received by exactly one subscriber.
 *
 * Developed just for fun, in contrast to widespread belief that reactive streams are hard to implement :).
 */
public class ThreadPublisher extends Thread implements Publisher<Long> {
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
            } while(elements > -1);
        } catch (InterruptedException e) {
        }
    }

    class SimpleSubscription extends Thread implements Subscription {
        Semaphore permits = new Semaphore(0);
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
            permits.release((int) Math.min(Integer.MAX_VALUE, n));
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
                while (!cancelled) {
                    permits.acquire(1);
                    Long res = output.take();
                    if (res.intValue() == -1) {
                        output.put(res); // for other subscribers
                        subscriber.onComplete();
                        return;
                    }
                    subscriber.onNext(res);
                }
            } catch (InterruptedException e) {
            }
        }
    }
}
