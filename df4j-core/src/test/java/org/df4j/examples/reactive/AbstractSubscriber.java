package org.df4j.examples.reactive;

public abstract class AbstractSubscriber<T> implements Flow.Subscriber<T> {
	Flow.Subscription subscription;
	final long bufferSize;
	long count;

	AbstractSubscriber(long bufferSize) {
		this.bufferSize = bufferSize;
	}

	public void onSubscribe(Flow.Subscription subscription) {
		long initialRequestSize = bufferSize;
		count = bufferSize - bufferSize / 2; // re-request when half consumed
		(this.subscription = subscription).request(initialRequestSize);
	}

	public void onNext(T item) {
		if (--count <= 0)
			subscription.request(count = bufferSize - bufferSize / 2);
		act(item);
	}

	public void onError(Throwable ex) {
		ex.printStackTrace();
	}

	public void onComplete() {
	}

	protected abstract void act(T item);
}