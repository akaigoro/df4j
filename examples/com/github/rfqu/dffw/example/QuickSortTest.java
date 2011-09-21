/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.dffw.example;

import static org.junit.Assert.fail;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;

import com.github.rfqu.dffw.core.*;
import com.github.rfqu.dffw.util.IntAggregator;

/**
 * Demonstrates how a recursive algorithm could be refactored into a network of tasks.
 * The key idea is that each task is combined with appropriate dataflow object,
 * which collects tokens from this task and its childs, and report result to the parent.
 * Here class IntAggregator was chosen for synchronization.
 * 
 * The Quicksort code originates from 
 * http://www.vogella.de/articles/JavaAlgorithmsQuicksort/article.html
 */
class QuickSort extends Task {
    IntAggregator result;
    int[] numbers;
    int low;
    int high;

    QuickSort(Port<Integer> caller, int[] numbers, int low, int high) {
        result=new IntAggregator(2, caller);
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
            result.send(0);            
            result.send(1);            
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
        // imitate recursion
        ArrayList<QuickSort> subtasks = new ArrayList<QuickSort>();
        if (low < j) {
            subtasks.add(new QuickSort(result, numbers, low, j));
        }
        if (i < high) {
            subtasks.add(new QuickSort(result, numbers, i, high));
        }
        switch (subtasks.size()) {
        case 2: 
            // run one subtask in parallel and another sequentially
            subtasks.get(0).fire();
            subtasks.get(1).run();
            break;
        case 1:
            result.send(0);        
            // run single subtask sequentially
            subtasks.get(0).run();
            break;
        case 0:
            // no subtasks, but 2 tokens must always be send for proper synchronization
            result.send(0);        
            result.send(1);        
        }
    }

}

public class QuickSortTest {
    static final int THRESHOLD = 10000;
    private final static int SIZE = 10000000;
    private final static int RANGE = Integer.MAX_VALUE;

    private int[] numbers;
    PrintStream out = System.out;
    private int nThreads = Runtime.getRuntime().availableProcessors();

    @Before
    public void setUp() {
        long startTime = System.currentTimeMillis();
        numbers = new int[SIZE];
        Random generator = new Random(137);
        for (int i = 0; i < numbers.length; i++) {
            numbers[i] = generator.nextInt(RANGE);
        }
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
//        out.println("generated "+numbers.length+" in " + elapsedTime+" ms");
    }

    public void runQuickSort(Executor executor, String name)
            throws InterruptedException {
        // out.println("Using "+Worker.class.getCanonicalName());
        long startTime = System.currentTimeMillis();

        Promise<Integer> sink = new Promise<Integer>();
        QuickSort sorter = new QuickSort(sink, numbers);
        executor.execute(sorter);
        int tasknum = sink.get();

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println(name + " " + elapsedTime + " ms; tasknum=" + tasknum);
        validate(numbers);
    }

    @Test
    public void testQuickSort() throws InterruptedException {
        // out.println("Using "+Worker.class.getCanonicalName());
        SimpleExecutorService executor = new SimpleExecutorService();
        runQuickSort(executor, "testQuickSort");
        executor.shutdown();
        validate(numbers);
    }

    @Test
    public void testQuickSortPar() throws InterruptedException {
        // out.println("Using "+ExecutorService.class.getCanonicalName());
        ThreadFactoryTL tf = new ThreadFactoryTL();
        ExecutorService executor = Executors.newFixedThreadPool(nThreads, tf);
        tf.setExecutor(executor);
        runQuickSort(executor, "testQuickSortPar");
        executor.shutdown();
        validate(numbers);
    }

    @Test
    public void testStandardSort() {
        long startTime = System.currentTimeMillis();
        Arrays.sort(numbers);
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        out.println("Standard Java sort " + elapsedTime+" ms");
    }

    private boolean validate(int[] numbers) {
        for (int i = 0; i < numbers.length - 1; i++) {
            if (numbers[i] > numbers[i + 1]) {
                fail("at " + i + ": " + numbers[i] + ", " + numbers[i + 1]);
            }
        }
        return true;
    }

    public static void main(String args[]) throws InterruptedException {
        QuickSortTest qt = new QuickSortTest();
        qt.setUp();
        qt.testQuickSortPar();
        qt.setUp();
        qt.testQuickSort();
        qt.setUp();
        qt.testStandardSort();
    }
}

