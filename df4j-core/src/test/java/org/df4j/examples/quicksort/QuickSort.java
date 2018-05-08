package org.df4j.examples.quicksort;

import org.df4j.core.connector.messagescalar.ScalarCollector;
import org.df4j.core.node.AsyncTask;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Demonstrates how a recursive algorithm could be refactored into a network of tasks.
 * The key idea is that each task is combined with appropriate dataflow object,
 * which collects tokens from this task and its childs, and report result to the parent.
 * Here class IntAggregator was chosen for synchronization.
 *
 * The Quicksort code originates from
 * http://www.vogella.de/articles/JavaAlgorithmsQuicksort/article.html
 */
class QuickSort extends AsyncTask {
    IntAggregator result;
    int[] numbers;
    int low;
    int high;

    QuickSort(ScalarCollector<Integer> caller, int[] numbers, int low, int high) {
        result=new IntAggregator(2, caller);
        this.numbers = numbers;
        this.low = low;
        this.high = high;
        // out.println(" from "+low+" to "+high);
    }

    public QuickSort(ScalarCollector<Integer> caller, int[] numbers) {
        this(caller, numbers, 0, numbers.length - 1);
    }

    private void exchange(int i, int j) {
        int temp = numbers[i];
        numbers[i] = numbers[j];
        numbers[j] = temp;
    }

    @Override
    public void act() {
        // do not parallelize sorting small arrays
        if ((high - low) < QuickSortTest.THRESHOLD) {
            Arrays.sort(numbers, low, high + 1);
            result.post(0);
            result.post(1);
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
            result.post(0);
            // run single subtask sequentially
            subtasks.get(0).run();
            break;
        case 0:
            // no subtasks, but 2 tokens must always be post for proper synchronization
            result.post(0);
            result.post(1);
        }
    }

}
