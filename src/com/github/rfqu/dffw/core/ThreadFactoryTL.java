/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.dffw.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;


/**
 * Allows standard java.util.concurrent executor to be used as a context executor
 * <p>
 * Usage:
 * <pre>
 *     	ThreadFactoryTL tf=new ThreadFactoryTL();
 *      ExecutorService executor = Executors.newFixedThreadPool(nThreads, tf);
 *      tf.setExecutor(executor);
 * </pre>            
 * @author rfq
 *
 */
public class ThreadFactoryTL implements ThreadFactory {
    ExecutorService executor;

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r) {
            @Override
            public void run() {
                Actor.setCurrentExecutor(executor);
                super.run();
            }
        };
    }
}