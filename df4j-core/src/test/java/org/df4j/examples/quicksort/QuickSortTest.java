/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.examples.quicksort;

import org.df4j.core.node.messagescalar.SimpleFuture;
import org.df4j.core.util.SameThreadExecutor;
import org.junit.Before;
import org.junit.Test;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import static org.junit.Assert.fail;

public class QuickSortTest {
    static final int THRESHOLD = 10000;
    private final static int SIZE = 10000000;
    private final static int RANGE = Integer.MAX_VALUE;

    private int[] numbers;
    PrintStream out = System.out;

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
            throws InterruptedException, ExecutionException {
        // out.println("Using "+Worker.class.getCanonicalName());
        long startTime = System.currentTimeMillis();

        SimpleFuture<Integer> sink = new SimpleFuture<Integer>();
        QuickSort sorter = new QuickSort(sink, numbers);
        executor.execute(sorter);
        int tasknum = sink.get();

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println(name + " " + elapsedTime + " ms; tasknum=" + tasknum);
        validate(numbers);
    }

    @Test
    public void testQuickSort() throws InterruptedException, ExecutionException {
        // out.println("Using "+Worker.class.getCanonicalName());
        SameThreadExecutor executor = new SameThreadExecutor();
        runQuickSort(executor, "testQuickSort");
        validate(numbers);
    }

    @Test
    public void testQuickSortPar() throws InterruptedException, ExecutionException {
        runQuickSort(ForkJoinPool.commonPool(), "testQuickSortPar");
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
}

