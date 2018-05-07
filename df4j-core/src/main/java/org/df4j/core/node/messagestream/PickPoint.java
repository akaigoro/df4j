package org.df4j.core.node.messagestream;

import org.df4j.core.connector.messagestream.StreamCollector;
import org.df4j.core.connector.messagestream.StreamInput;
import org.df4j.core.connector.reactivestream.Subscription;
import org.df4j.core.node.messagescalar.AbstractPromise;

public class PickPoint<M> extends AbstractPromise implements StreamCollector<M> {
	/** place for input token(s) */
    protected final StreamInput<M> resources = new StreamInput<>(this);
    protected Subscription subscription;

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
    protected Object getToken() {
        return this.resources.current();
    }
}
