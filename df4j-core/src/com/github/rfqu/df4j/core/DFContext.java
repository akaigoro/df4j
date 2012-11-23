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
 * Creates threads marked with an Executor, allowing standard java.util.concurrent
 * executors to be used as context executors.
 * @author rfq
 *
 */
public class DFContext  {
	
	protected static final ThreadLocal <DFContext> currentContextKey = new ThreadLocal <DFContext> () {
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

//=========================
    static String dfprefix = " DF ";
    
    protected Executor newDefaultExecutor() {
		int nThreads=Runtime.getRuntime().availableProcessors();
		return newFixedThreadPool(nThreads);
	}   
    
    protected Executor newSingleThreadExecutor() {
    	ThFactory tf = new ThFactory(dfprefix);
	    return Executors.newSingleThreadExecutor(tf);
	}
    
    protected ThreadPoolExecutor newFixedThreadPool(int nThreads) {
    	ThFactory tf = new ThFactory(dfprefix);
	    return (ThreadPoolExecutor) Executors.newFixedThreadPool(nThreads, tf);
	}
    
    protected ThreadPoolExecutor newCachedThreadPool() {
    	ThFactory tf = new ThFactory(dfprefix);
	    return (ThreadPoolExecutor) Executors.newCachedThreadPool(tf);
    }
    
    public static void setSingleThreadExecutor() {
    	DFContext context=getCurrentContext();
	    Executor executor = context.newSingleThreadExecutor();
	    context.setCurrentExecutor(executor);
	}
    
    public static void setFixedThreadPool(int nThreads) {
    	DFContext context=getCurrentContext();
	    Executor executor = context.newFixedThreadPool(nThreads);
	    context.setCurrentExecutor(executor);
	}
    
    public static void setCachedThreadPool() {
    	DFContext context=getCurrentContext();
	    Executor executor = context.newCachedThreadPool();
	    context.setCurrentExecutor(executor);
    }
    
    class ThFactory extends ThreadGroup implements ThreadFactory {
        String prefix;

    	public ThFactory(String prefix) {
    	    super(dfprefix);
            this.prefix=prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            return new ThreadTL(r);
        }

        class ThreadTL extends Thread {
    	    
            public ThreadTL(Runnable r) {
                super(ThFactory.this, r);
                setDaemon(true);
            }

            @Override
            public void run() {
            	setCurrentContext(DFContext.this);
                super.run();
            }
        }
    }
}