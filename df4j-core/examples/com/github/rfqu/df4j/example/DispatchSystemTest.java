/**
 * Copyright (c) 2008-2009 Apple Inc. All rights reserved.
 * Copyright (C) 2009-2010, Progress Software Corporation and/or its
 * subsidiaries or affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rfqu.df4j.example;

import java.io.PrintStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.Test;

import com.github.rfqu.df4j.core.*;

import static java.lang.String.format;

/**
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class DispatchSystemTest {
    PrintStream out = System.out;
    final static int nThreads = Runtime.getRuntime().availableProcessors();

    static int PARTITIONS = 1000;
    static int WARM_UP_ITERATIONS = 10000;
    static int RUN_UP_ITERATIONS = 1000*1000*10;
    
    ExecutorService executor;
    
    @After
    public void shutdown() {
        executor.shutdown();
    }

    @Test
    public void testS() throws InterruptedException {
        executor = new SimpleExecutorService();
        benchmark();
    }

    @Test
    public void testJUC() throws InterruptedException {
        ThreadFactoryTL tf = new ThreadFactoryTL();
        executor = Executors.newFixedThreadPool(nThreads, tf);
        tf.setExecutor(executor);
        benchmark();
    }

    static class MyTask extends Task {
        final CountDownLatch counter;
        final ExecutorService executor;
        
        public MyTask(ExecutorService executor, CountDownLatch counter) {
            this.executor = executor;
            this.counter = counter;
        }

        public void run() {
            counter.countDown();
            if( counter.getCount()>0 ) {
                executor.execute(this);
            }
        }
        
    }
    static void execute(final ExecutorService executor, int iterations) throws InterruptedException {
        final CountDownLatch counter = new CountDownLatch(iterations);
        for (int i = 0; i < 1000; i++) {
            executor.execute(new MyTask(executor, counter));
        }
        counter.await();

    }

    private void benchmark() throws InterruptedException {
        String name = executor.getClass().getSimpleName();
        System.out.println(format("warm up: %s", name));
        execute(executor, WARM_UP_ITERATIONS);
        System.out.println(format("benchmarking: %s", name));
        long start = System.nanoTime();
        execute(executor, RUN_UP_ITERATIONS);
        long end = System.nanoTime();
        double durationS = 1.0d*(end-start)/1000000000d;
        double rate = RUN_UP_ITERATIONS / durationS;
        System.out.println(format("name: %s, duration: %,.3f s, rate: %,.2f executions/sec", name, durationS, rate));
    }

    public static void main(String[] args) throws Exception {
        new DispatchSystemTest().testS();
        new DispatchSystemTest().testJUC();    }
}
