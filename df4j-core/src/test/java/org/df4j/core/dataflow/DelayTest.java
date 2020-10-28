package org.df4j.core.dataflow;

import org.df4j.core.actor.Actor;
import org.df4j.core.connector.Completion;
import org.df4j.core.port.OutFlow;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import static org.junit.Assert.assertTrue;

public class DelayTest {

    @Test
    public void delayTest() throws InterruptedException {
        DelayActor actor = new DelayActor();
        DelaySubscriber subscriber = new DelaySubscriber();
        actor.out.subscribe(subscriber);
        actor.start();
        assertTrue(subscriber.await(1000));
    }

    static class DelaySubscriber extends Completion implements Subscriber<Integer> {
        Subscription s;

        @Override
        public void onSubscribe(Subscription s) {
            this.s = s;
            s.request(1);
        }

        @Override
        public void onNext(Integer o) {
            System.out.println("got "+o);
            s.request(1);
        }

        @Override
        public void onError(Throwable t) {
            System.out.println("got "+t);
            completeExceptionally(t);
        }

        @Override
        public void onComplete() {
            System.out.println("completed");
            complete();
        }
    }
    static class DelayActor extends Actor {
        OutFlow<Integer> out = new OutFlow<>(this) ;
        int period = 100;
        int cnt = 0;
        int maxValue = 4;

        @Override
        protected void runAction() throws Throwable {
            out.onNext(-1);
            if (cnt % 2 ==0) {
                int value = cnt / 2;
                out.onNext(value);
                if (value == maxValue) {
                    complete();
                    out.onComplete();
                    return;
                }
            }
            cnt++;
            delay(period);
        }
    }
}
