package org.df4j.core.ext;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.df4j.core.Actor1;
import org.junit.Assert;
import org.junit.Test;

public class ZeroThreadExecutorTest {
	ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
	ZeroThreadExecutor executor0 = new ZeroThreadExecutor(scheduler);
	int count = 0;
	AtomicInteger atomicCount = new AtomicInteger(0);

	class CyclingActor extends Actor1<Integer> {
		Random rand = new Random();
		int msgCount=0;
		Runnable command = ()->{
			this.post(1);
		};
		
		public CyclingActor() {
			setExecutor(executor0);
		}

		@Override
		protected void act(Integer message) throws Exception {
			int locCount = count;
			int millis = 10+rand.nextInt(20);
			Thread.sleep(millis);
			count = locCount+1;
			atomicCount.incrementAndGet();
			if ((++msgCount) == 5) {
				return;
			}
			millis = 10+rand.nextInt(20);
			scheduler.schedule(command, millis, TimeUnit.MILLISECONDS);
		}

	}
	
	@Test
	public void test1() throws InterruptedException {
		CyclingActor[] actors = new CyclingActor[4];
		for (int k=0; k<actors.length; k++) {
			CyclingActor actor = new CyclingActor();
			actors[k] = actor;
			actor.post(1);
		}
		Thread.sleep(500);
		scheduler.shutdown();
		scheduler.awaitTermination(1, TimeUnit.HOURS);
		int expected = atomicCount.get();
		System.out.println("expected="+expected+" count="+count);
		Assert.assertEquals(expected, count);
	}
}
