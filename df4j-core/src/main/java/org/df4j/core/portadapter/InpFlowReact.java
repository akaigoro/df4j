package org.df4j.core.portadapter;

import org.df4j.core.dataflow.BasicBlock;
import org.df4j.core.port.InpFlow;
import org.df4j.core.portadapter.Flow2ReactiveSubscriber;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class InpFlowReact<T> extends InpFlow implements Subscriber<T> {
    Flow2ReactiveSubscriber<T> proxy;

    public InpFlowReact(BasicBlock parent) {
        super(parent);
        proxy = new Flow2ReactiveSubscriber(this);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        proxy.onSubscribe(subscription);
    }
}
