package com.github.rfq.dffw.example;

import com.github.rfq.dffw.core.Port;
import com.github.rfq.dffw.core.Task;

/***************************************************************************
 * Quicksort code from http://www.vogella.de/articles/JavaAlgorithmsQuicksort/article.html
 **************************************************************************/

public class QuickSort extends Task implements Port<Integer> {
	int[] numbers;
	int low;
	int high;
	
	QuickSort(int[] numbers, int low, int high) {
		this.numbers=numbers;
		this.low=low;
		this.high=high;
//		System.out.println("  from "+low+" to "+high);
	}
	
	public QuickSort(int[] numbers) {
		this(numbers, 0, numbers.length - 1);
	}

	private void exchange(int i, int j) {
		int temp = numbers[i];
		numbers[i] = numbers[j];
		numbers[j] = temp;
	}
	
	@Override
	public void run() {
		int i = low, j = high;
		// Get the pivot element from the middle of the list
		int pivot = numbers[low + (high-low)/2];

		// Divide into two lists
		while (i <= j) {
			// If the current value from the left list is smaller then the pivot
			// element then get the next element from the left list
			while (numbers[i] < pivot) {
				i++;
			}
			// If the current value from the right list is larger then the pivot
			// element then get the next element from the right list
			while (numbers[j] > pivot) {
				j--;
			}

			// If we have found a values in the left list which is larger then
			// the pivot element and if we have found a value in the right list
			// which is smaller then the pivot element then we exchange the
			// values.
			// As we are done we can increase i and j
			if (i <= j) {
				exchange(i, j);
				i++;
				j--;
			}
		}
		// Recursion
		if (low < j) {
			QuickSort ps = new QuickSort(numbers, low, j);
			ps.fire();
			ps.request(this);
		} else {
			send(0);
		}
		if (i < high) {
			QuickSort ps = new QuickSort(numbers, i, high);
			ps.fire();
			ps.request(this);
		} else {
			send(0);
		}
	}
	
	int maxcount=2;
	int eventCount=0;
	int value=1;
	Port<Integer> caller;

	@Override
	public  Object send(Integer delta) {
		synchronized(this) {
			eventCount++;
			value+=delta;
			if (eventCount!=maxcount || caller==null) {
				return this;
			}
		}
		caller.send(new Integer(value));
		return this;
	}
	
	public synchronized void request(Port<Integer> port) {
		synchronized(this) {
			caller=port;
			if (maxcount>eventCount) {
				return;
			}
		}
		caller.send(new Integer(value));
	}
}

