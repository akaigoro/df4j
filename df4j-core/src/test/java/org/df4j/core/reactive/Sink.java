package org.df4j.core.reactive;

import org.df4j.core.Actor;

import java.util.concurrent.CountDownLatch;

/**
 * receives totalNumber of Integers and cancels the subscription
 */
class Sink extends Actor {
    int totalNumber;
    StreamSubscriber<Integer> sub;
    CountDownLatch fin = new CountDownLatch(1);
    int received = 0;

    public Sink(int totalNumber) {
        sub = new StreamSubscriber<Integer>(this, 5);
        this.totalNumber = totalNumber;
    }

    @Override
    protected void act() {
        Integer val = sub.get();
        ReactorTest.println("  Sink.get()="+val);
        if (val != null) {
            received++;
            if (received >= totalNumber) {
                sub.cancel();
            }
        } else { // stream closed
            ReactorTest.println("  countDown");
            fin.countDown();
        }
    }
}
