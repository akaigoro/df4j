package org.df4j.core.actor;

import org.df4j.core.port.InpFlow;
import org.df4j.core.port.OutFlow;
import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * To make concrete processor, the method {@link AbstractProcessor##atNext(Object)} need to be implemented
 * @param <T> type of processed data
 * @param <R> type of produced data
 */
public abstract class AbstractProcessor<T, R> extends Actor implements Processor<T,R> {

    private InpFlow<T> inPort = new InpFlow<>(this);
    private OutFlow<R> outPort = new OutFlow<>(this);

    public InpFlow<T> getInPort() {
        return inPort;
    }

    public OutFlow<R> getOutPort() {
        return outPort;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        inPort.onSubscribe(subscription);
    }

    @Override
    public void onNext(T item) {
        inPort.onNext(item);
    }

    @Override
    public void onError(Throwable throwable) {
        inPort.onError(throwable);
    }

    @Override
    public void onComplete() {
        inPort.onComplete();
    }

    @Override
    public void subscribe(Subscriber<? super R> subscriber) {
        outPort.subscribe(subscriber);
    }

    protected synchronized void whenComplete(Throwable throwable) {
        if (throwable == null) {
            outPort.onComplete();
        } else {
            outPort.onError(throwable);
        }
    }

    /**
     *
     * @param item input data
     * @return processed data
     * @throws Throwable if something went wrong
     */
    protected abstract R whenNext(T item)  throws Throwable;

    /** processes one data item
     */
    @Override
    protected void runAction() throws Throwable {
        if (inPort.isCompleted()) {
            Throwable completionException = inPort.getCompletionException();
            complete(completionException);
        } else {
            T item = inPort.poll();
            if (item==null) {
                throw new RuntimeException();
            }
            R res = whenNext(item);
            if (res == null) {
                complete(null);
            } else {
                outPort.onNext(res);
            }
        }
    }
}

