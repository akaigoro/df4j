package com.github.rfq.dffw.util;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;

import com.github.rfq.dffw.core.ThreadFactoryTL;
import com.github.rfq.dffw.core.Worker;
import com.github.rfq.dffw.example.*;

public class QuickSortTest {
	private int[] numbers;
	private final static int SIZE = 100000;
	private final static int MAX = Integer.MAX_VALUE;
	PrintStream out = System.out;
	private int nThreads=2;
    
	@Before
	public void setUp() throws Exception {
		numbers = new int[SIZE];
		Random generator = new Random();
		for (int i = 0; i < numbers.length; i++) {
			numbers[i] = generator.nextInt(MAX);
		}
	}

	@Test
	public void testQuickSort() throws InterruptedException {
	    Worker executor=new Worker();
		executor.start();
		long startTime = System.currentTimeMillis();

		QuickSort sorter = new QuickSort(numbers);
		executor.execute(sorter);
		Promise<Integer> sink=new Promise<Integer>();
		sorter.request(sink);
		int tasknum=sink.get();

		long stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime - startTime;
		System.out.println("Quicksort " + elapsedTime+" ms; tasknum="+tasknum);

		if (!validate(numbers)) {
			fail("Should not happen");
		}
	}

    @Test
    public void testQuickSortPar() throws InterruptedException {
    	out.println("Using "+ExecutorService.class.getCanonicalName());
    	ThreadFactoryTL tf=new ThreadFactoryTL();
        ExecutorService executor = Executors.newFixedThreadPool(nThreads, tf);
        tf.setExecutor(executor);
		long startTime = System.currentTimeMillis();

		QuickSort sorter = new QuickSort(numbers);
		executor.execute(sorter);
		Promise<Integer> sink=new Promise<Integer>();
		sorter.request(sink);
		int tasknum=sink.get();

		long stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime - startTime;
		System.out.println("QuicksortPar " + elapsedTime+" ms; tasknum="+tasknum);

		if (!validate(numbers)) {
			fail("Should not happen");
		}
        executor.shutdown();
    }

//    @Test
	public void testStandardSort() {
		long startTime = System.currentTimeMillis();
		Arrays.sort(numbers);
		long stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime - startTime;
		System.out.println("Standard Java sort " + elapsedTime);
		if (!validate(numbers)) {
			fail("Should not happen");
		}
		assertTrue(true);
	}

	private boolean validate(int[] numbers) {
		for (int i = 0; i < numbers.length - 1; i++) {
			if (numbers[i] > numbers[i + 1]) {
				return false;
			}
		}
		return true;
	}

	private void printResult(int[] numbers) {
		for (int i = 0; i < numbers.length; i++) {
			System.out.print(numbers[i]);
		}
		System.out.println();
	}

}
