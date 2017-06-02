package org.df4j.core.ext;

import java.util.concurrent.Executor;

import org.df4j.core.Actor;
import org.df4j.core.Actor1;

/**
 * Let we have a set of actors which share common mutable data structure.
 * If we had threads instead of actors, we would like to exclude simultaneous
 * access to the data structire by means of synchronized blocks or methods.
 * But actors may not block, to avoid thread starvation.
 * ZeroThreadExecutor allows to organize mutual exclusion of actor execution.
 * Just create an instanse of ZeroThreadExecutor and use it as an executor 
 * in all Actors belonging to the set.
 */
public class ZeroThreadExecutor extends Actor implements Executor {
	protected final StreamInput<Runnable> mainInput = new Actor.StreamInput<>();

	public ZeroThreadExecutor(Executor executor) {
		setExecutor(executor);
	}

	@Override
	public void execute(Runnable task) {
        mainInput.post(task);
	}

	@Override
	protected void act() throws Exception {
		try {
            Runnable task = mainInput.get();
			task.run();
		} catch (Throwable e) {
			e.printStackTrace();
		}	
	}

}
