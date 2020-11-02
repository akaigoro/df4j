package org.df4j.core.port;

import org.df4j.core.activities.LoggingSubscriber;
import org.df4j.core.activities.PublisherActor;
import org.df4j.core.actor.AbstractPublisher;
import org.df4j.core.actor.Actor;
import org.df4j.protocol.Flow;
import org.junit.Assert;
import org.junit.Test;
import org.reactivestreams.Subscriber;

public class PortGroupTest {

    @Test
    public void mergeTest1() throws InterruptedException {
        PublisherActor prod1 = new PublisherActor(3,40);
        PublisherActor prod2 = new PublisherActor(5,40);
        MergeActor1 merger1 = new MergeActor1();
        LoggingSubscriber subscriber = new LoggingSubscriber();
        prod1.subscribe(merger1.inp1);
        prod2.subscribe(merger1.inp2);
        merger1.subscribe(subscriber);
        prod1.start();
        prod2.start();
        merger1.start();
        //  subscriber.start();
        boolean ok = subscriber.await(1000);
        Assert.assertTrue(ok);
    }

    @Test
    public void mergeTest2() throws InterruptedException {
        PublisherActor prod1 = new PublisherActor(3,40);
        PublisherActor prod2 = new PublisherActor(5,40);
        MergeActor2 merger2 = new MergeActor2();
        LoggingSubscriber subscriber = new LoggingSubscriber();
        prod1.subscribe(merger2.inp1);
        prod2.subscribe(merger2.inp2);
        merger2.subscribe(subscriber);
        prod1.start();
        prod2.start();
        merger2.start();
        //  subscriber.start();
        boolean ok = subscriber.await(1000);
        Assert.assertTrue(ok);
    }
    /**
     * completes eagerly
     */
    static class MergeActor1 extends AbstractPublisher<Long> {
        PortGroup mport = new PortGroup(this);
        InpFlow<Long> inp1 = new InpFlow<>(mport);
        InpFlow<Long> inp2 = new InpFlow<>(mport);

        @Override
        protected Long whenNext() throws Throwable {
            if (inp1.isCompleted() || inp2.isCompleted()) {
                return null;
            }
            if (inp1.isReady()) {
                return inp1.remove();
            }
            if (inp2.isReady()) {
                return inp2.remove();
            }
            throw new IllegalStateException();
        }
    }

    /**
     * completes when both inputs complete
     * @param <T>
     */
    static class MergeActor2<T> extends Actor implements Flow.Publisher<Long> {
        protected OutFlow<Long> outPort = new OutFlow<>(this, 8);
        PortGroup mport = new PortGroup(this);
        InpFlow<Long> inp1 = new InpFlow<>(mport);
        InpFlow<Long> inp2 = new InpFlow<>(mport);

        @Override
        public void subscribe(Subscriber<? super Long> subscriber) {
            outPort.subscribe(subscriber);
        }

        protected synchronized void whenComplete() {
            outPort.onComplete();
        }

        protected synchronized void whenComplete(Throwable throwable) {
            if (throwable == null) {
                outPort.onComplete();
            } else {
                outPort.onError(throwable);
            }
        }

        /** generates one data item
         */
        @Override
        protected void runAction() throws Throwable {
            if (inp1.isCompleted() && inp2.isCompleted()) {
                complete(null);
                return;
            }
            if (inp1.isCompleted()) {
                inp1.block();
            } else if (inp1.isReady()) {
                outPort.onNext(inp1.remove());
            }
            if (inp2.isCompleted()) {
                inp2.block();
            } else if (inp2.isReady()) {
                outPort.onNext(inp2.remove());
            }
        }
    }
}