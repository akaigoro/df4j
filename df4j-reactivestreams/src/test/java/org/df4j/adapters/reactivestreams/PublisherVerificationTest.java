package org.df4j.adapters.reactivestreams;

import org.df4j.core.activities.PublisherActor;
import org.df4j.core.dataflow.Actor;
import org.df4j.core.dataflow.Dataflow;
import org.df4j.core.port.OutFlow;
import org.df4j.core.util.Logger;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.TestEnvironment;

import java.lang.reflect.Field;
import java.util.logging.Level;

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
        ErrPublisherActor publisher = new ErrPublisherActor();
        publisher.start();
        return publisher.out;
    }

    public class ErrPublisherActor extends Actor {
        public OutFlow<Long> out = new OutFlow<>(this){
            @Override
            public void subscribe(Subscriber<? super Long> subscriber) {
                super.subscribe(new Subscriber<Long>(){
                    @Override
                    public void onSubscribe(org.reactivestreams.Subscription s) {
                        subscriber.onSubscribe(s);
                        s.request(1);
                    }

                    @Override
                    public void onNext(Long aLong) {
                        subscriber.onNext(aLong);
                    }

                    @Override
                    public void onError(Throwable t) {
                        subscriber.onError(t);
                    }

                    @Override
                    public void onComplete() {
                        subscriber.onComplete();
                    }
                });
            }
        };

        @Override
        protected void runAction() throws Throwable {
            out.onError(new RuntimeException());
            stop();
        }
    }

}
