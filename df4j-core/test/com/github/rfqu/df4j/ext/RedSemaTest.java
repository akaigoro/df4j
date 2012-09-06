package com.github.rfqu.df4j.ext;

import org.junit.Test;

import com.github.rfqu.df4j.core.Task;

/**
 * Computes Farey sequence.
 * Demonstrates using implicit Executor via Task class.
 * Demonstrates using Reduceable Semaphore to wait a set of relative tasks to finish,
 *  when the number of tasks is unknown.
 * Based on
 * https://github.com/relgames/FareyGenerator/blob/master/src/main/java/org/relgames/test/ExecutorFarey.java
 * @author rfqu
 */
public class RedSemaTest {
	static final int BASE = 500;
	
	/** Farey fraction.
	 */
	static class Node {
		public int numerator;
		public int denominator;
		public Node next;

		public Node(int num, int den) {
			numerator=num;
			denominator=den;
		}
	}
	
    /** 
     * Counts running tasks to wait for.
     * Initially there are no tasks, so permits=1 in order not to hang on aquire.
     */
	RedSema sema = new RedSema(1);

	class SubTask extends Task {
		private final Node left;
		private final Node right;

		public SubTask(Node left, Node right) {
			this.left = left;
			this.right = right;
			// when a new task appear, reduce permits to wait for the task end.
			sema.reducePermits();
		}

		public void start() {
			super.fire();
		}
		
		@Override
		public void run() {
			int den = left.denominator + right.denominator;
			if (den > BASE) {
				left.next=right;
			} else {
				Node mediant = new Node(left.numerator + right.numerator, den);
				new SubTask(mediant, right).start();
				new SubTask(left, mediant).start();
			}
			// when this task finishes, release permit
			sema.release();
		}
	}
	
	@Test
	public void test() throws InterruptedException {
		long time = System.currentTimeMillis();
		Node left = new Node(0, 1);
		Node right = new Node(1, 1);

		SubTask task = new SubTask(left, right);
		task.start();
		// wait for the task and its descendants to finish
		sema.acquire();
		
		int max = 0;
		int size = 0;
		for (Node node = left; node != null; node = node.next) {
			size++;
			int denominator = node.denominator;
			if (denominator > max) {
				max = denominator;
			}
		}

		System.out.printf("Total %d, max=%d in  %dms\n", size, max,
				System.currentTimeMillis() - time);
	}

	public static void main(String[] args) throws InterruptedException {
		for (int k = 0; k < 7; k++) {
			new RedSemaTest().test();
		}
	}
}
