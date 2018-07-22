package org.df4j.core.simplenode.messagescalar;

import org.df4j.core.boundconnector.messagescalar.AsyncResult;
import org.df4j.core.boundconnector.messagescalar.ScalarSubscriber;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CompletedResult<M> implements AsyncResult<M> {
	public final M value;
	public final Throwable exception;

	public CompletedResult(M value) {
		this.value = value;
		this.exception = null;
	}

	public CompletedResult(M value, Throwable exception) {
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

    @Override
    public Future<M> asFuture() {
        return this;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public M get() throws InterruptedException, ExecutionException {
        if (exception == null) {
            return value;
        } else {
            throw new ExecutionException(exception);
        }
    }

    @Override
    public M get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return get();
    }
}
