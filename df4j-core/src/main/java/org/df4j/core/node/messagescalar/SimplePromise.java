package org.df4j.core.node.messagescalar;

import org.df4j.core.connector.messagescalar.ConstInput;
import org.df4j.core.connector.messagescalar.ScalarSubscriber;
import org.df4j.core.connector.messagescalar.SimpleSubscription;
import org.df4j.core.node.Action;

public class SimplePromise<M> extends AbstractPromise<M> implements ScalarSubscriber<M> {
    protected final ConstInput<M> result = new ConstInput<>(this);
    {
        start();
    }

    @Override
    public void post(M message) {
        result.post(message);
    }

    @Override
    public void postFailure(Throwable throwable) {
        result.postFailure(throwable);
    }

    @Override
    public void onSubscribe(SimpleSubscription subscription) {
        result.onSubscribe(subscription);
    }

    @Action
    public void act(ScalarSubscriber<? super M> request, M result) {
        request.post(result);
    }

}
