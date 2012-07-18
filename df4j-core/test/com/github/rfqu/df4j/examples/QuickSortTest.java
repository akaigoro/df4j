/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.examples;

import static org.junit.Assert.fail;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import org.junit.Before;
import org.junit.Test;

import com.github.rfqu.df4j.core.*;

/**
 * Demonstrates how a recursive algorithm could be refactored into a network of tasks.
 * The key idea is that each task is combined with appropriate dataflow object,
 * which collects tokens from this task and its childs, and reports result to the parent.
 * Here class IntAggregator was chosen for synchronization.
 * 
 * The Quicksort code originates from 
 * http://www.vogella.de/articles/JavaAlgorithmsQuicksort/article.html
 */
class QuickSort extends Task {
	Port<Integer> caller;
    int[] numbers;
    int low;
    int high;

    QuickSort(Port<Integer> caller, int[] numbers, int low, int high) {
    	this.caller=caller;
        this.numbers = numbers;
        this.low = low;
        this.high = high;
        // out.println(" from "+low+" to "+high);
    }

    public QuickSort(Port<Integer> caller, int[] numbers) {
        this(caller, numbers, 0, numbers.length - 1);
    }

    private void exchange(int i, int j) {
        int temp = numbers[i];
        numbers[i] = numbers[j];
        numbers[j] = temp;
    }

    @Override
    public void run() {
        // do not parallelize sorting small arrays 
        if ((high - low) < QuickSortTest.THRESHOLD) {
            Arrays.sort(numbers, low, high + 1);
            caller.send(1);            
            return;
        }
        // split the array into two subarrays and process them in parallel
        int i = low, j = high;
        int pivot = numbers[low + (high - low) / 2];
        while (i <= j) {
            while (numbers[i] < pivot) {
                i++;
            }
            while (numbers[j] > pivot) {
                j--;
            }
            if (i <= j) {
                exchange(i, j);
                i++;
                j--;
            }
        }
    	IntAggregator result=new IntAggregator(caller);
    	int resultcount=0;
        // imitate recursion
        if (low < j) {
            new QuickSort(result, numbers, low, j).fire();
            resultcount++;
        }
        if (i < high) {
            new QuickSort(result, numbers, i, high).fire();
            resultcount++;
        }
        result.setCount(resultcount);
    }

    static class IntAggregator  implements Port<Integer> {
        Port<Integer> port;
        int maxCount=Integer.MAX_VALUE;
        int eventCount = 0;
        int value = 0;

        public IntAggregator(Port<Integer> caller) {
            this.port = caller;
        }

        @Override
        public void send(Integer delta) {
            synchronized (this) {
                eventCount++;
                value += delta;
                if (eventCount < maxCount) {
                    return;
                }
            }
            port.send(new Integer(value));
        }

        public void setCount(int resultcount) {
            synchronized (this) {
                maxCount=resultcount;
                if (eventCount < maxCount) {
                    return;
                }
            }
            port.send(new Integer(value));
        }
    }
}

public class QuickSortTest {
    static final int THRESHOLD = 10000;
    static final int SIZE = 10000000;
    static final int RANGE = Integer.MAX_VALUE/16;
    static final int[] numbers = new int[SIZE];
    static final PrintStream out = System.out;
    static final int nThreads = Runtime.getRuntime().availableProcessors();

    @Before
    public void setUp() {
        Random generator = new Random(137);
        for (int i = 0; i < numbers.length; i++) {
            numbers[i] = generator.nextInt(RANGE);
        }
    }

    public void runQuickSort(Executor executor) throws InterruptedException, ExecutionException {
        out.println("Using "+executor.getClass().getCanonicalName());
        long startTime = System.currentTimeMillis();

        CallbackFuture<Integer> sink = new CallbackFuture<Integer>();
        QuickSort sorter = new QuickSort(sink, numbers);
        executor.execute(sorter);
        int tasknum = sink.get();

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println(" elapsed=" + elapsedTime + " ms; tasknum=" + tasknum);
        validate(numbers);
    }

    void validate(int[] numbers) {
        for (int i = 0; i < numbers.length - 1; i++) {
            if (numbers[i] > numbers[i + 1]) {
                fail("at " + i + ": " + numbers[i] + ", " + numbers[i + 1]);
            }
        }
    }

    @Test
    public void testQuickSort() throws InterruptedException, ExecutionException {
        runQuickSort(ThreadFactoryTL.newSingleThreadExecutor());
    }

    @Test
    public void testQuickSortPar() throws InterruptedException, ExecutionException {
        runQuickSort(ThreadFactoryTL.newFixedThreadPool(nThreads));
    }

    @Test
    public void testStandardSort() {
        long startTime = System.currentTimeMillis();
        Arrays.sort(numbers);
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        out.println("Standard Java sort " + elapsedTime+" ms");
    }

    public static void main(String args[]) throws InterruptedException, ExecutionException {
        QuickSortTest qt = new QuickSortTest();
        qt.setUp();
        qt.testQuickSortPar();
        qt.setUp();
        qt.testQuickSort();
        qt.setUp();
        qt.testStandardSort();
    }
}

