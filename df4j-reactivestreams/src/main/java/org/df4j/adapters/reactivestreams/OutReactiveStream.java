package org.df4j.adapters.reactivestreams;

import org.df4j.core.dataflow.BasicBlock;
import org.df4j.core.port.OutFlow;
import org.df4j.protocol.Flow;
import org.df4j.protocol.FlowSubscription;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class OutReactiveStream<T> extends OutFlow<T> implements Publisher<T> {
    /**
     * @param parent {@link BasicBlock} to which this port belongs
     */
    public OutReactiveStream(BasicBlock parent) {
        super(parent);
    }

    @Override
    public void subscribe(Subscriber s) {
        super.subscribe(new ProxySubscriber<T>(s));
    }

    /** acts also as a Subscription */
    static class ProxySubscriber<T> implements Flow.Subscriber<T>, Subscription {
        final Subscriber subscriber;
        FlowSubscription flowSubscription;

        public ProxySubscriber(Subscriber subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void onSubscribe(FlowSubscription s) {
            flowSubscription = s;
            subscriber.onSubscribe(this);
        }

        @Override
        public void onNext(T m) {
            subscriber.onNext(m);
        }

        @Override
        public void onError(Throwable t) {
            subscriber.onError(t);
        }

        @Override
        public void onComplete() {
            subscriber.onComplete();
        }

        @Override
        public void request(long n) {
            flowSubscription.request(n);
        }

        @Override
        public void cancel() {
            flowSubscription.cancel();
        }
    }
}
