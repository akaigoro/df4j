/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.core;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Contains:
 * - default executor
 * - threadfactory for context-aware executors, including default one
 * - timer 
 * Is accessed as a Threadlocal variable
 * @author rfq
 */
class DFContext6  {
	
    protected static final ThreadLocal <DFContext> currentContextKey
        = new ThreadLocal <DFContext> ()
    {
		@Override
		protected DFContext initialValue() {
			return new DFContext();
		}   	
    };

    /**
     */
    public static void setCurrentContext(DFContext context) {
        currentContextKey.set(context);
    }

    /**
     * @return current context stored in thread-local variable
     */
    public static DFContext getCurrentContext() {
        return currentContextKey.get();
    }

    /**
     * removes current context
     */
    public static void removeCurrentContext() {
        currentContextKey.remove();
    }
    
	//----------------------- ----------------- Executor
    protected Executor currentExecutor;
    /**
     * @return current executor stored in thread-local variable
     */
	protected synchronized Executor _getCurrentExecutor() {
		Executor res = currentExecutor;
		if (res == null) {
			res=currentExecutor = newDefaultExecutor();
		}
		return res;
    }

    /**
     * @return current executor stored in thread-local variable
     */
    public static Executor getCurrentExecutor() {
    	return getCurrentContext()._getCurrentExecutor();
    }

	/** 
	 * Do it on your own risk.
	 * Good practice is that your executor should spread this context on its threads.
	 * @return old executorremoveCurrentExecutor
	 */
    public synchronized Executor setCurrentExecutor(Executor executor) {
    	Executor res = currentExecutor;
		currentExecutor=executor;
		return res;
	}

    //---------------------------------------- context-aware thread factory

    static String dfprefix = " DF ";
    
    protected Executor newDefaultExecutor() {
        int nThreads=Runtime.getRuntime().availableProcessors();
        return newFixedThreadPool(nThreads);
    }   
    
    protected Executor newSingleThreadExecutor() {
        ContextThreadFactory tf = new ContextThreadFactory(dfprefix);
        return Executors.newSingleThreadExecutor(tf);
    }
    
    protected ThreadPoolExecutor newFixedThreadPool(int nThreads) {
        ContextThreadFactory tf = new ContextThreadFactory(dfprefix);
        return (ThreadPoolExecutor) Executors.newFixedThreadPool(nThreads, tf);
    }
    
    protected ThreadPoolExecutor newCachedThreadPool() {
        ContextThreadFactory tf = new ContextThreadFactory(dfprefix);
        return (ThreadPoolExecutor) Executors.newCachedThreadPool(tf);
    }
    
    public static void setSingleThreadExecutor() {
        DFContext6 context=getCurrentContext();
        Executor executor = context.newSingleThreadExecutor();
        context.setCurrentExecutor(executor);
    }
    
    public static void setFixedThreadPool(int nThreads) {
        DFContext6 context=getCurrentContext();
        Executor executor = context.newFixedThreadPool(nThreads);
        context.setCurrentExecutor(executor);
    }
    
    public static void setCachedThreadPool() {
        DFContext6 context=getCurrentContext();
        Executor executor = context.newCachedThreadPool();
        context.setCurrentExecutor(executor);
    }
    
    class ContextThreadFactory extends ThreadGroup implements ThreadFactory {
        String prefix;

        public ContextThreadFactory(String prefix) {
            super(dfprefix);
            this.prefix=prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            return new ThreadTL(r);
        }

        class ThreadTL extends Thread {
            
            public ThreadTL(Runnable r) {
                super(ContextThreadFactory.this, r);
                setDaemon(true);
            }

            @Override
            public void run() {
                setCurrentContext((DFContext) DFContext6.this);
                super.run();
            }
        }
    }

    //----------------------- ----------------- ExecutorService

    protected ExecutorService executorService;

    protected synchronized ExecutorService _getCurrentExecutorService() {
        if (executorService==null) {
            Executor executor=_getCurrentExecutor();
            if (executor instanceof ExecutorService) {
                executorService=(ExecutorService)executor;
            } else {
                executorService=new PrimitiveExecutorService(executor);
            }
        }
        return executorService;
    }

    public static ExecutorService getCurrentExecutorService() {
        return getCurrentContext()._getCurrentExecutorService();
    }

    /**
     * Waits for currently started tasks to finish.
     * Invoke before exiting main thread, or otherwise
     * thread pool with daemon threads would break execution
     * of the not finished tasks.
     */
    protected synchronized void _completeCurrentExecutorService() {
        if (!(currentExecutor==null)) {
            return;
        }
        executorService=null;
        if (!(currentExecutor instanceof ExecutorService)) {
            currentExecutor=null;
            return;
        }
        ExecutorService service = (ExecutorService)currentExecutor;
        currentExecutor=null;
        service.shutdown();
        try {
            service.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
        service.shutdownNow();
    }
    
    /**
     * Waits for currently started tasks to finish.
     * Invoke before exiting main thread, or otherwise
     * thread pool with daemon threads would break execution
     * of the not finished tasks.
     */
    public static void completeCurrentExecutorService() {
        getCurrentContext()._completeCurrentExecutorService();
    }
    
    static class PrimitiveExecutorService implements ExecutorService {
        static final String message = "PrimitiveExecutor not a service";
        protected final Executor executor;

        public PrimitiveExecutorService(Executor executor) {
            this.executor = executor;
        }

        /**
         * Executes the given command at some time in the future.
         * 
         * @param command the runnable
         * @throws NullPointerException if command is null
         */
        @Override
        public void execute(Runnable command) {
            executor.execute(command);
        }

        @Override
        public void shutdown() {
            throw new UnsupportedOperationException(message);
            
        }

        @Override
        public List<Runnable> shutdownNow() {
            throw new UnsupportedOperationException(message);
        }

        @Override
        public boolean isShutdown() {
            throw new UnsupportedOperationException(message);
        }

        @Override
        public boolean isTerminated() {
            throw new UnsupportedOperationException(message);
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit)
                throws InterruptedException {
            throw new UnsupportedOperationException(message);
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            throw new UnsupportedOperationException(message);
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            throw new UnsupportedOperationException(message);
        }

        @Override
        public Future<?> submit(Runnable task) {
            throw new UnsupportedOperationException(message);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
                throws InterruptedException {
            throw new UnsupportedOperationException(message);
        }

        @Override
        public <T> List<Future<T>> invokeAll(
                Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                throws InterruptedException {
            throw new UnsupportedOperationException(message);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
                throws InterruptedException, ExecutionException {
            throw new UnsupportedOperationException(message);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
                long timeout, TimeUnit unit) throws InterruptedException,
                ExecutionException, TimeoutException {
            throw new UnsupportedOperationException(message);
        }

    }

    //----------------------- ----------------- Timer
    protected Timer currentTimer;
    /**
     * @return current executor stored in thread-local variable
     */
    protected synchronized Timer _getCurrentTimer() {
		Timer res = currentTimer;
		if (res == null) {
			res=currentTimer = Timer.newTimer(this);
		}
		return res;
    }

    /**
     * @return current executor stored in thread-local variable
     */
    public static Timer getCurrentTimer() {
    	return getCurrentContext()._getCurrentTimer();
    }

    protected synchronized boolean _removeTimer(Timer timer) {
		if (timer == currentTimer) {
			currentTimer = null;
			return true;
		}
		return false;
    }

    public static  boolean removeTimer(Timer timer) {
    	return getCurrentContext()._removeTimer(timer);
    }
}