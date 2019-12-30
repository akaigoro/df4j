package org.df4j.core.portadapter;

import org.df4j.core.dataflow.BasicBlock;
import org.df4j.core.port.InpFlow;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class InpReact<T> implements Subscriber<T> {
    final InpFlow<T> delegatePort;
    Flow2ReactiveSubscriber<T> proxy;

    public InpReact(BasicBlock parent) {
        delegatePort = new InpFlow<>(parent);
        proxy = new Flow2ReactiveSubscriber(delegatePort);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        proxy.onSubscribe(subscription);
    }

    public T remove() {
        return delegatePort.remove();
    }

    public boolean isCompleted() {
        return  delegatePort.isCompleted();
    }

    public Throwable getCompletionException() {
        return delegatePort.getCompletionException();
    }

    @Override
    public void onNext(T message) {
        delegatePort.onNext(message);
    }

    @Override
    public void onError(Throwable t) {
        delegatePort.onError(t);
    }

    @Override
    public void onComplete() {
        delegatePort.onComplete();
    }

    public boolean isCompletedExceptionslly() {
        return delegatePort.isCompletedExceptionslly();
    }
}
