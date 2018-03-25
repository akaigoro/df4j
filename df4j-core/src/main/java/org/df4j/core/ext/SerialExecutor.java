package org.df4j.core.ext;

import java.util.concurrent.Executor;

/**
 * Let we have a set of actors which share common mutable data structure.
 * If we had threads instead of actors, we would like to exclude simultaneous
 * access to the data structure by means of synchronized blocks or methods.
 * But actors may not block, to avoid thread starvation.
 * SerialExecutor allows to organize mutual exclusion of actor execution.
 * Just create an instanse of SerialExecutor and use it as an executor
 * in all Actors belonging to the set.
 */
public class SerialExecutor extends Actor1<Runnable> implements Executor {

	public SerialExecutor(Executor executor) {
		setExecutor(executor);
	}

	@Override
	public void execute(Runnable task) {
        post(task);
	}

	@Override
	protected void act(Runnable task) throws Exception {
		try {
			task.run();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
