package org.df4j.examples.reactive;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.df4j.examples.reactive.Flow.*;

class OneShotPublisher implements Publisher<Boolean> {
	private final ExecutorService executor = ForkJoinPool.commonPool(); // daemon-based
	private boolean subscribed; // true after first subscribe

	public synchronized void subscribe(Subscriber<? super Boolean> subscriber) {
		if (subscribed)
			subscriber.onError(new IllegalStateException()); // only one allowed
		else {
			subscribed = true;
			subscriber
					.onSubscribe(new OneShotSubscription(subscriber, executor));
		}
	}

	public void join() throws InterruptedException {
		executor.awaitTermination(3, TimeUnit.SECONDS);
	}
	
	static class OneShotSubscription implements Subscription {
		private final Subscriber<? super Boolean> subscriber;
		private final ExecutorService executor;
		private Future<?> future; // to allow cancellation
		private boolean completed;

		OneShotSubscription(Subscriber<? super Boolean> subscriber,
				ExecutorService executor) {
			this.subscriber = subscriber;
			this.executor = executor;
		}

		public synchronized void request(long n) {
			if (n != 0 && !completed) {
				completed = true;
				if (n < 0) {
					IllegalArgumentException ex = new IllegalArgumentException();
					executor.execute(() -> subscriber.onError(ex));
				} else {
					future = executor.submit(() -> {
						subscriber.onNext(Boolean.TRUE);
						subscriber.onComplete();
					});
				}
			}
		}

		public synchronized void cancel() {
			completed = true;
			if (future != null)
				future.cancel(false);
		}
	}
}