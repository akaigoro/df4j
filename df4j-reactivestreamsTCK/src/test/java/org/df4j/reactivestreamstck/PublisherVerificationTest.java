package org.df4j.reactivestreamstck;

import org.df4j.core.actor.Actor;
import org.df4j.core.port.OutFlow;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.TestEnvironment;

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

    private static class MyPublisherActor extends Actor {
        public OutFlow<Long> out = new OutFlow<>(this);

        @Override
        protected void runAction() {
            out.onError(new RuntimeException());
            complete();
        }
    }
}
