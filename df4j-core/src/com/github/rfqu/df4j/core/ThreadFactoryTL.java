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
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;


/**
 * Allows standard java.util.concurrent executors to be used as context executors
 * <p>
 * @author rfq
 *
 */
public class ThreadFactoryTL implements ThreadFactory {
    static String dfprefix = " DF ";
    String prefix;
    Executor executor;

	public ThreadFactoryTL(String prefix, Executor executor) {
        this.executor=executor;
        this.prefix=prefix;
    }

    public ThreadFactoryTL(String prefix) {
        this.prefix=prefix;
    }

    protected void setExecutor(Executor executor) {
        this.executor = executor;
    }

    @Override
    public Thread newThread(Runnable r) {
        return new ThreadTL(r);
    }
    
    public static Executor newSingleThreadExecutor() {
		ThreadFactoryTL tf = new ThreadFactoryTL(dfprefix);
	    Executor executor = Executors.newSingleThreadExecutor(tf);
	    tf.setExecutor(executor);
		return executor;
	}
    
    public static ThreadPoolExecutor newFixedThreadPool(int nThreads) {
		ThreadFactoryTL tf = new ThreadFactoryTL(dfprefix);
		ThreadPoolExecutor executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(nThreads, tf);
	    tf.setExecutor(executor);
		return executor;
	}
    
    public static ThreadPoolExecutor newCachedThreadPool() {
		ThreadFactoryTL tf = new ThreadFactoryTL(dfprefix);
		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool(tf);
	    tf.setExecutor(executor);
		return executor;
	}
    
	public class ThreadTL extends Thread {
	    
        public ThreadTL(Runnable r) {
            super(r);
            setName(getName()+prefix+executor.getClass().getSimpleName());
            setDaemon(true);
        }

        @Override
        public void run() {
            Task.setCurrentExecutor(executor);
            super.run();
        }
    }
}