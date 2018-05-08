package org.df4j.core.node.messagestream;

import org.df4j.core.connector.messagescalar.SimpleSubscription;
import org.df4j.core.connector.messagestream.StreamCollector;
import org.df4j.core.connector.messagestream.StreamInput;
import org.df4j.core.connector.messagestream.StreamSubscriber;
import org.df4j.core.connector.reactivestream.Subscription;
import org.df4j.core.node.messagescalar.AbstractPromise;

public class PickPoint<M> extends AbstractPromise<M> implements StreamSubscriber<M> {
	/** place for input token(s) */
    protected final StreamInput<M> resources = new StreamInput<>(this);
    protected SimpleSubscription subscription;

	@Override
	public void post(M resource) {
    	resources.post(resource);
    }

	@Override
	public void postFailure(Throwable t) {
		resources.postFailure(t);
	}

	@Override
	public void complete() {
		stop();
	}

	@Override
    protected M getToken() {
        return this.resources.current();
    }

	@Override
	public void onSubscribe(SimpleSubscription subscription) {
		this.subscription = subscription;
	}
}
