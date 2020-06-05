package org.df4j.reactivestreamstck;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.dataflow.Dataflow;
import org.df4j.core.port.OutFlow;
import org.df4j.core.util.Logger;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.TestEnvironment;

import java.util.logging.Level;

public class PublisherVerificationTest extends org.reactivestreams.tck.PublisherVerification<Long> {
    static final long defaultTimeout = 400;

    public PublisherVerificationTest() {
        super(new TestEnvironment(defaultTimeout));
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        LoggingPublisherActor publisher = new LoggingPublisherActor(elements);
        publisher.start();
        return publisher;
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        MyPublisherActor publisher = new MyPublisherActor();
        publisher.start();
        return publisher.out;
    }

    // todo remove
    public void stochastic_spec103_mustSignalOnMethodsSequentially() throws Throwable {
        super.stochastic_spec103_mustSignalOnMethodsSequentially();
    }

    static class LoggingPublisherActor extends Actor implements Publisher<Long> {
        protected final Logger logger = new Logger(this);
        final int delay;
        public OutFlow<Long> out;
        public long cnt;

        {
            setLogLevel(Level.OFF);
        }

        public LoggingPublisherActor(long elements) {
            this(elements, 0);
            setLogLevel(Level.OFF);
        }

        public LoggingPublisherActor(long cnt, int delay) {
            this(new Dataflow(), cnt, delay);
        }

        public LoggingPublisherActor(Dataflow parent, long cnt, int delay) {
            this(parent, cnt, delay, OutFlow.DEFAULT_CAPACITY);
        }

        public LoggingPublisherActor(Dataflow parent, long cnt, int delay, int capacity) {
            super(parent);
            out = new OutFlow<>(this, capacity);
            this.cnt = cnt;
            this.delay = delay;
            logger.info("PublisherActor: cnt = " + cnt);
        }

        @Override
        public void subscribe(Subscriber<? super Long> s) {
            logger.info("PublisherActor.subscribe:");
            out.subscribe(new ProxySubscriber(s));
        }

        public void setLogLevel(Level off) {
            logger.setLevel(off);
        }

        @Override
        protected void runAction() throws Throwable {
            if (cnt > 0) {
                logger.info("PublisherActor.onNext(" + cnt+")");
                out.onNext(cnt);
                cnt--;
                Thread.sleep(delay);
            } else {
                logger.info("PublisherActor.onComplete");
                out.onComplete();
                complete();
            }
        }

        private class ProxySubscription implements Subscription {
            private org.reactivestreams.Subscription subscription;

            public ProxySubscription(org.reactivestreams.Subscription subscription) {
                this.subscription = subscription;
            }

            @Override
            public void request(long n) {
                logger.info("    Subscription.request:"+n);
                subscription.request(n);
            }

            @Override
            public void cancel() {
                logger.info("    Subscription.cancel:");
                subscription.cancel();
            }
        }

        class ProxySubscriber implements Subscriber<Long> {
            private final Subscriber<? super Long> sub;

            public ProxySubscriber(Subscriber<? super Long> s) {
                sub = s;
            }

            @Override
            public void onSubscribe(org.reactivestreams.Subscription subscription) {
                logger.info("        Subscriber.onSubscribe");
                ProxySubscription proxy = new ProxySubscription(subscription);
                sub.onSubscribe(proxy);
            }

            @Override
            public void onError(Throwable t) {
                logger.info("        Subscriber.onError:"+t);
                sub.onError(t);
            }

            @Override
            public void onComplete() {
                logger.info("        Subscriber.onComplete");
//                new Exception().printStackTrace();
                sub.onComplete();
            }

            @Override
            public void onNext(Long t) {
                logger.info("        Subscriber.onNext:"+t);
                sub.onNext(t);
            }
        }
    }

    private static class MyPublisherActor extends Actor {
        public OutFlow<Long> out = new OutFlow<>(this);

        @Override
        protected void runAction() {
            out.onError(new RuntimeException());
            complete();
        }
    }
}
