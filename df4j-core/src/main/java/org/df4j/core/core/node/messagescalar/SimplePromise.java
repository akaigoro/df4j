package org.df4j.core.core.node.messagescalar;

import org.df4j.core.core.connector.messagescalar.ConstInput;
import org.df4j.core.core.connector.messagescalar.ScalarSubscriber;
import org.df4j.core.core.connector.messagescalar.SimpleSubscription;

public class SimplePromise<M> extends AbstractPromise<M> implements ScalarSubscriber<M> {
    protected final ConstInput<M> result = new ConstInput<>(this);

    @Override
    public void post(M message) {
        result.post(message);
    }

    @Override
    public void postFailure(Throwable throwable) {
        result.postFailure(throwable);
    }

    protected M getToken() {
        return this.result.current();
    }

    @Override
    public void onSubscribe(SimpleSubscription subscription) {
        result.onSubscribe(subscription);
    }
}
