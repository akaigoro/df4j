package org.df4j.core.actor;

import org.df4j.core.port.InpFlow;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Actor according to Carl Hewitt: has single predefined input port.
 * Additional ports can be added, though.
 * @param <T> Type of input messages
 */
public abstract class AbstractSubscriber<T> extends Actor implements Subscriber<T> {
    public InpFlow<T> inp = new InpFlow<>(this);
    private volatile MessageAction<T> nextMessageAction = this::whenNext;

    public AbstractSubscriber(ActorGroup parent) {
        super(parent);
    }

    public AbstractSubscriber() { }

    public InpFlow<T> getInPort() {
        return inp;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        inp.onSubscribe(subscription);
    }

    @Override
    public void onNext(T item) {
        inp.onNext(item);
    }

    @Override
    public void onError(Throwable throwable) {
        inp.onError(throwable);
    }

    @Override
    public void onComplete() {
        inp.onComplete();
    }

    protected abstract void whenNext(T item) throws Throwable;

    /** processes one data item
     */
    @Override
    protected void runAction() throws Throwable {
        if (inp.isCompleted()) {
            Throwable completionException = inp.getCompletionException();
            complete(completionException);
        } else {
            T token = inp.remove();
            this.nextMessageAction.runAction(token); // initially it is call to #whenNext
        }
    }

    protected void nextMessageAction(MessageAction<T> messageAction) {
        this.nextMessageAction = messageAction;
    }

    @FunctionalInterface
    public interface MessageAction<T> {
        void runAction(T message) throws Throwable;
    }
}
