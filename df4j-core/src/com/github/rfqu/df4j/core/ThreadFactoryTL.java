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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;


/**
 * Allows standard java.util.concurrent executor to be used as a context executor
 * <p>
 * @author rfq
 *
 */
public class ThreadFactoryTL implements ThreadFactory {
    ExecutorService executor;

    public ThreadFactoryTL() {
	}

    public ThreadFactoryTL(ExecutorService executor) {
		this.executor=executor;
	}

	protected void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public Thread newThread(Runnable r) {
        return new ThreadTL(r);
    }
    
    public static ExecutorService newFixedThreadPool(int nThreads) {
		ThreadFactoryTL tf = new ThreadFactoryTL();
	    ExecutorService executor = Executors.newFixedThreadPool(nThreads, tf);
	    tf.setExecutor(executor);
		return executor;
	}
    
    public static ExecutorService newSingleThreadExecutor() {
		ThreadFactoryTL tf = new ThreadFactoryTL();
	    ExecutorService executor = Executors.newSingleThreadExecutor(tf);
	    tf.setExecutor(executor);
		return executor;
	}
    
    public static ExecutorService newSingleThreadExecutor(ExecutorService executor) {
		return Executors.newSingleThreadExecutor(new ThreadFactoryTL(executor));
	}
    
	public class ThreadTL extends Thread {
        public ThreadTL(Runnable r) {
            super(r);
            setName(getName()+" DF "+executor.getClass().getSimpleName());
        }

        @Override
        public void run() {
            Task.setCurrentExecutor(executor);
            super.run();
        }
    }
}