package org.df4j.tck;


import org.df4j.core.actor.MulticastSource;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;

import java.lang.reflect.Field;

public class ReactiveStreamMulticastPublisherTest extends PublisherVerification<Long> {
    static final  int defaultTimeout = 400;
    private final TestEnvironment env;

    public ReactiveStreamMulticastPublisherTest() throws NoSuchFieldException, IllegalAccessException {
        super(new TestEnvironment(defaultTimeout));

        Field env = PublisherVerification.class.getDeclaredField("env");
        env.setAccessible(true);
        this.env = (TestEnvironment) env.get(this);
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        MulticastSource flowPublisher = new MulticastSource(elements);
        flowPublisher.start();
        return flowPublisher;
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        Publisher<Long> flowPublisher = new FailedUnicastSource();
        return flowPublisher;
    }

    static class FailedUnicastSource extends MulticastSource {
        @Override
        public void subscribe(Subscriber<? super Long> subscriber) {
            super.subscribe(subscriber);
            subscriber.onError(new RuntimeException());
        }

        @Override
        protected void runAction() {
            pub.onError(new RuntimeException());
        }
    }

}
