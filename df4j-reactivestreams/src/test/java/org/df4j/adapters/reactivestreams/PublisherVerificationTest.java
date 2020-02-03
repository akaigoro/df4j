package org.df4j.adapters.reactivestreams;

import org.df4j.core.activities.PublisherActor;
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
        PublisherActor publisher = new PublisherActor(0){
            @Override
            protected void runAction() {
                out.onError(new RuntimeException());
                stop();
            }
        };
        publisher.start();
        return publisher.out;
    }

    // todo remove
    public void stochastic_spec103_mustSignalOnMethodsSequentially() throws Throwable {
        super.stochastic_spec103_mustSignalOnMethodsSequentially();
    }

    static class LoggingPublisherActor extends PublisherActor implements Publisher<Long> {

        public LoggingPublisherActor(long elements) {
            super(elements);
            setLogLevel(Level.OFF);
        }

        @Override
        public void subscribe(Subscriber<? super Long> s) {
            logger.info("PublisherActor.subscribe:");
            out.subscribe(new ProxySubscriber(s));
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
}
