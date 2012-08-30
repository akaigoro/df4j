package com.github.rfqu.df4j.ext;

import java.util.concurrent.Semaphore;

/**
 * Reduceable Semaphore.
 * The number of permits can be negative, which can be interpreted as
 * number of vetoes (prohibitions).
 * CountdownLatch is a similar synchronization facility, 
 * but does not support increasing the number of vetoes.  
 * @author kaigorodov
 *
 */
public class RedSema extends Semaphore {
	private static final long serialVersionUID = 1L;

	public RedSema(int permits) {
    	super(permits);
    }

	@Override
	public void reducePermits(int reduction) {
		super.reducePermits(reduction);
	}

	public void reducePermits() {
		super.reducePermits(1);
	}

}