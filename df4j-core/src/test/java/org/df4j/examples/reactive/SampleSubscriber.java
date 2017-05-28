package org.df4j.examples.reactive;

public class SampleSubscriber extends AbstractSubscriber<Boolean> {

	SampleSubscriber(long bufferSize) {
		super(bufferSize);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void act(Boolean item) {
		System.out.println("i="+item);
	}

	@Override
	public void onComplete() {
		System.out.println("the end");
	}

	public static void main(String... args) throws InterruptedException {
		OneShotPublisher pub = new OneShotPublisher();
		SampleSubscriber sub = new SampleSubscriber(10);
		pub.subscribe(sub);
		pub.join();
	}
}