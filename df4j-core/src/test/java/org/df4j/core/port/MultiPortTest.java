package org.df4j.core.port;

import org.df4j.core.activities.LoggingSubscriber;
import org.df4j.core.activities.PublisherActor;
import org.df4j.core.dataflow.Actor;
import org.junit.Assert;
import org.junit.Test;

public class MultiPortTest {

    @Test
    public void mergeTest1() {
        PublisherActor prod1 = new PublisherActor(3,40);
        PublisherActor prod2 = new PublisherActor(5,40);
        MergeActor1 merger = new MergeActor1();
        LoggingSubscriber subscriber = new LoggingSubscriber();
        prod1.out.subscribe(merger.inp1);
        prod2.out.subscribe(merger.inp2);
        merger.out.subscribe(subscriber);
        prod1.start();
        prod2.start();
        merger.start();
        //  subscriber.start();
        boolean ok = subscriber.blockingAwait(1000);
        Assert.assertTrue(ok);
    }

    @Test
    public void mergeTest2() {
        PublisherActor prod1 = new PublisherActor(3,40);
        PublisherActor prod2 = new PublisherActor(5,40);
        MergeActor2 merger = new MergeActor2();
        LoggingSubscriber subscriber = new LoggingSubscriber();
        prod1.out.subscribe(merger.inp1);
        prod2.out.subscribe(merger.inp2);
        merger.out.subscribe(subscriber);
        prod1.start();
        prod2.start();
        merger.start();
        //  subscriber.start();
        boolean ok = subscriber.blockingAwait(1000);
        Assert.assertTrue(ok);
    }
    /**
     * completes eagerly
     *
     * @param <T>
     */
    static class MergeActor1<T> extends Actor {
        MultiPort mport = new MultiPort(this);
        InpFlow<T> inp1 = new InpFlow<>(mport);
        InpFlow<T> inp2 = new InpFlow<>(mport);
        OutFlow<T> out = new OutFlow<>(this);

        @Override
        protected void runAction() throws Throwable {
            if (inp1.isCompleted() || inp2.isCompleted()) {
                complete();
                out.onComplete();
                return;
            }
            if (inp1.isReady()) {
                out.onNext(inp1.remove());
            }
            if (inp2.isReady()) {
                out.onNext(inp2.remove());
            }
        }
    }

    static class MergeActor2<T> extends MergeActor1 {

        @Override
        protected void runAction() throws Throwable {
            if (inp1.isCompleted() && inp2.isCompleted()) {
                complete();
                out.onComplete();
                return;
            }
            if (inp1.isReady() && !inp1.isCompleted()) {
                out.onNext(inp1.remove());
            }
            if (inp2.isReady() && !inp2.isCompleted()) {
                out.onNext(inp2.remove());
            }
        }
    }
}