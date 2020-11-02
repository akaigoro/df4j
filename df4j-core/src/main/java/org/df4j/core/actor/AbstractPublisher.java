package org.df4j.core.actor;

import org.df4j.core.port.OutFlow;
import org.df4j.protocol.Flow;
import org.reactivestreams.Subscriber;

/**
 * minimalistic {@link Publisher} implementation.
 * Only one subscriber can subscribe.
 * @param <T> type of produced data
 */
public abstract class AbstractPublisher<T> extends Actor implements Flow.Publisher<T> {
    protected final OutFlow<T> outPort;

    public AbstractPublisher(ActorGroup parent, int capacity) {
        super(parent);
        outPort = new OutFlow<>(this, capacity);
    }

    public AbstractPublisher() {
        this(new ActorGroup(), 8);
    }

    public OutFlow<T> getOutPort() {
        return outPort;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        outPort.subscribe(subscriber);
    }

    protected synchronized void whenComplete() {
        outPort.onComplete();
    }

    protected synchronized void whenComplete(Throwable throwable) {
        if (throwable == null) {
            outPort.onComplete();
        } else {
            outPort.onError(throwable);
        }
    }

    protected abstract T whenNext()  throws Throwable;

    /** generates one data item
     */
    @Override
    protected void runAction() throws Throwable {
        T res = whenNext();
        if (res == null) {
            complete(null);
        } else {
            outPort.onNext(res);
        }
    }
}
