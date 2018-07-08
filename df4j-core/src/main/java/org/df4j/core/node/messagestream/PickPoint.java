package org.df4j.core.node.messagestream;

import org.df4j.core.connector.messagescalar.ScalarSubscriber;
import org.df4j.core.connector.messagestream.StreamInput;
import org.df4j.core.connector.messagestream.StreamPublisher;
import org.df4j.core.connector.messagestream.StreamSubscriber;
import org.df4j.core.node.Action;
import org.df4j.core.node.Actor1;

public class PickPoint<M> extends Actor1<M> implements StreamPublisher<M> {
	/** place for input demands */
	protected final StreamInput<StreamSubscriber<? super M>> requests = new StreamInput<>(this);
	{
		start();
	}

	@Override
	public <S extends StreamSubscriber<? super M>> S subscribe(S subscriber) {
		requests.post(subscriber);
		return subscriber;
	}

	@Action
	protected void act(ScalarSubscriber<? super M> request, M resource) {
		request.post(resource);
	}
}
