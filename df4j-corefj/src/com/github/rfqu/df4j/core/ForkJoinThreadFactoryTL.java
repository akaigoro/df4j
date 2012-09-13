package com.github.rfqu.df4j.core;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;

public class ForkJoinThreadFactoryTL  implements ForkJoinWorkerThreadFactory {
    String prefix;
    Executor executor;

    public ForkJoinThreadFactoryTL(String dfprefix) {
        this.prefix=dfprefix;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    @Override
    public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
        return new ForkJoinWorkerThreadTL(pool);
    }
    
    public static ForkJoinPool newForkJoinPool() {
        return newForkJoinPool(Runtime.getRuntime().availableProcessors());
    }
    
    public static ForkJoinPool newForkJoinPool(int parallelism) {
        ForkJoinThreadFactoryTL tf = new ForkJoinThreadFactoryTL(" FJP ");
        ForkJoinPool executor = new ForkJoinPool(parallelism, tf, null, false);
        tf.setExecutor(executor);
        return executor;
    }
    
    class ForkJoinWorkerThreadTL extends ForkJoinWorkerThread {
        protected ForkJoinWorkerThreadTL(ForkJoinPool pool) {
            super(pool);
            setName(getName()+prefix+pool.getClass().getSimpleName());
            setDaemon(true);
        }

        @Override
        protected void onStart() {
            super.onStart();
            Task.setCurrentExecutor(executor);
        }
    }
    
}