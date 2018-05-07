package org.df4j.core.node.messagescalar;

import org.df4j.core.connector.messagescalar.ScalarPublisher;
import org.df4j.core.connector.messagescalar.ScalarSubscriber;

public class CompletedPromise<M> implements ScalarPublisher<M> {
	public final M value;

	public CompletedPromise(M value) {
		this.value = value;
	}

	@Override
	public <S extends ScalarSubscriber<? super M>> S subscribe(S subscriber) {
		subscriber.post(value);
		return subscriber;
	}
}
