package org.df4j.core.connector.messagescalar;

import org.df4j.core.connector.messagescalar.ScalarPublisher;
import org.df4j.core.connector.messagescalar.ScalarSubscriber;

public class CompletedPromise<M> implements ScalarPublisher<M> {
	public final M value;
	public final Throwable exception;

	public CompletedPromise(M value) {
		this.value = value;
		this.exception = null;
	}

	public CompletedPromise(M value, Throwable exception) {
	    if (value != null) {
	        throw new IllegalArgumentException("first argument must be null in the two-argument constructor");
        }
		this.value = null;
		this.exception = exception;
	}

	@Override
	public <S extends ScalarSubscriber<? super M>> S subscribe(S subscriber) {
	    if (exception == null) {
            subscriber.post(value);
        } else {
	        subscriber.postFailure(exception);
        }
		return subscriber;
	}
}
