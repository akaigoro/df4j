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

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 *  Tasks themselves are messages and can be send to other Actors and Ports.
 * @author kaigorodov
 *
 */
public abstract class Task extends Link implements Runnable {
    protected final Executor executor;

    public Task(Executor executor) {
        this.executor = executor;
    }

    public Task() {
        this(getCurrentExecutorService());
    }

    /**
     * activates this task by sending it to the executor
     */
    protected final void fire() {
        if (executor==null) {
            run();
        } else {
            executor.execute(this);
        }
    }

    private static final ThreadLocal <ExecutorService> currentExecutorServiceKey = new ThreadLocal <ExecutorService> () {
		@Override
		protected ExecutorService initialValue() {
			int nThreads=Runtime.getRuntime().availableProcessors();
			return ThreadFactoryTL.newFixedThreadPool(nThreads);
		}   	
    };

	/**
     * sets current executor as a thread-local variable
     * @param executor
     */
    public static void setCurrentExecutorService(ExecutorService executor) {
    	currentExecutorServiceKey.set(executor);
    }

    /**
     * @return current executor stored in thread-local variable
     */
    public static ExecutorService getCurrentExecutorService() {
        return currentExecutorServiceKey.get();
    }

    /**
     * retrieves current executor as a thread-local variable
     */
    public static void removeCurrentExecutorService() {
        currentExecutorServiceKey.remove();
    }
}