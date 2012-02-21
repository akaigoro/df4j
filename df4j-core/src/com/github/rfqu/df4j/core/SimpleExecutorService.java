package com.github.rfqu.df4j.core;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/** 
 * incomplete
 *
 */
public class SimpleExecutorService extends AbstractExecutorService {
    SimpleExecutor worker = new SimpleExecutor();

    public SimpleExecutorService() {
        worker.start();
    }
    
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return false; // TODO
    }

    @Override
    public boolean isShutdown() {
        return worker.isClosed();
    }

    @Override
    public boolean isTerminated() {
        return worker.isCompleted();
    }

    /**
     * Initiates an orderly shutdown in which previously submitted tasks are executed, 
     * but no new tasks will be accepted.
     * Invocation has no additional effect if already shut down. 
     */
    @Override
    public synchronized void shutdown() {
        worker.close();
    }

    @Override
    public List<Runnable> shutdownNow() {
        shutdown();
        return null; // TODO
    }

    @Override
    public void execute(Runnable command) {
        worker.execute(command);
    }

    class SimpleExecutor extends FatActor<Task> implements Executor {
    	{
    		thread.setName(thread.getName() + " DF SimpleExecutorService");
    		executor=SimpleExecutorService.this;
    	}

    	/**
    	 * Executes the given command at some time in the future. The command
    	 * executes in the underlying thread.
    	 * 
    	 * @param <command>
    	 *            the runnable task Throws: RejectedExecutionException if this
    	 *            task cannot be accepted for execution. NullPointerException if
    	 *            command is null
    	 */
    	@Override
    	public void execute(Runnable command) {
    		try {
    			if (command instanceof Task) {
    				send((Task) command);
    			} else {
    				send(new TaskWrapper(command));
    			}
    		} catch (NullPointerException e) {
    			throw e;
    		} catch (Exception e) {
    			throw new RejectedExecutionException(e);
    		}
    	}

    	@Override
    	protected void act(Task task) throws Exception {
    		task.run();
    	}

    	@Override
    	protected void complete() throws Exception {
    		// TODO Auto-generated method stub

    	}

    	@Override
    	protected void failure(Task message, Exception e) {
    		// TODO Auto-generated method stub
    		super.failure(message, e);
    		thread.interrupt();
    	}

    }

	static class TaskWrapper extends Task {
	    Runnable command;

	    public TaskWrapper(Runnable command) {
	        this.command = command;
	    }

	    @Override
	    public void run() {
	        command.run();
	    }
	}
}
