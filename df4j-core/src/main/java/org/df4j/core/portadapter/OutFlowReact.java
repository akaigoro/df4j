package org.df4j.core.portadapter;

import org.df4j.core.dataflow.BasicBlock;
import org.df4j.core.port.OutFlow;
import org.df4j.protocol.Flow;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

public class OutFlowReact<T> extends OutFlow<T> implements Publisher<T> {
    /**
     * @param parent {@link BasicBlock} to which this port belongs
     */
    public OutFlowReact(BasicBlock parent, int bufferCapacity) {
        super(parent,bufferCapacity);
    }

    /**
     * @param parent {@link BasicBlock} to which this port belongs
     */
    public OutFlowReact(BasicBlock parent) {
        super(parent);
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        Flow.Subscriber proxy = new Reactive2FlowSubscriber(subscriber);
        super.subscribe(proxy);
    }
}
