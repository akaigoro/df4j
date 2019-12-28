package org.df4j.core.portadapter;

import org.df4j.core.dataflow.BasicBlock;
import org.df4j.core.port.OutFlow;
import org.df4j.protocol.Flow;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

public class OutReact<T> implements Publisher<T> {
    final OutFlow<T> delegatePort;
    /**
     * @param parent {@link BasicBlock} to which this port belongs
     */
    public OutReact(BasicBlock parent) {
        delegatePort = new OutFlow(parent);
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        Flow.Subscriber proxy = new Reactive2FlowSubscriber(subscriber);
        delegatePort.subscribe(proxy);
    }

    public void onNext(T message) {
        delegatePort.onNext(message);
    }

    public void onComplete() {
        delegatePort.onComplete();
    }

    public void onError(Throwable t) {
        delegatePort.onError(t);
    }
}
