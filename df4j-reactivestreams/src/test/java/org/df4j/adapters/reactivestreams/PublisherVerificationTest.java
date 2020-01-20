package org.df4j.adapters.reactivestreams;

import org.df4j.core.activities.PublisherActor;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.TestEnvironment;

import java.lang.reflect.Field;

public class PublisherVerificationTest extends org.reactivestreams.tck.PublisherVerification {
    static final long defaultTimeout = 400;

    public PublisherVerificationTest() {
        super(new TestEnvironment(defaultTimeout));
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        PublisherActor publisher = new PublisherActor(elements);
        publisher.start();
        return publisher.out;
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
}
