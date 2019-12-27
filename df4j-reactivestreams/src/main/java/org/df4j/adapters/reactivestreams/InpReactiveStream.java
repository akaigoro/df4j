package org.df4j.adapters.reactivestreams;

import org.df4j.core.dataflow.BasicBlock;
import org.df4j.core.port.InpFlow;
import org.df4j.protocol.FlowSubscription;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class InpReactiveStream<T> extends InpFlow<T> implements Subscriber<T> {
    public InpReactiveStream(BasicBlock parent) {
        super(parent);
    }

    @Override
    public void onSubscribe(Subscription s) {
        FlowableSubscription flowableSubscription = new FlowableSubscription(s);
        super.onSubscribe(flowableSubscription);
    }

    static class FlowableSubscription implements FlowSubscription {
        private final Subscription subscription;
        private boolean isCancelled = false;

        public FlowableSubscription(Subscription subscription) {
            this.subscription = subscription;
        }

        @Override
        public void request(long n) {
            subscription.request(n);
        }

        @Override
        public void cancel() {
            isCancelled = true;
            subscription.cancel();
        }

        @Override
        public boolean isCancelled() {
            return isCancelled;
        }
    }
}
