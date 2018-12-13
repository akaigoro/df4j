package org.df4j.core.util.asyncmon;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayDeque;

public class ProducerConsumerSync {

    static class BlockingQ<T> {
        private final int maxItems;
        private final ArrayDeque<T> buf;
        private int count = 0;

        public BlockingQ(int maxItems) {
            this.maxItems = maxItems;
            buf = new ArrayDeque(maxItems);
        }

        public synchronized void put(T item) throws InterruptedException {
            while (count == maxItems) {
                wait();
            }
            buf.add(item);
            count++;
            notifyAll();
        }

        public T take() throws InterruptedException {
            while (count == 0) {
                wait();
            }
            T item = buf.poll();
            count--;
            notifyAll();
            return item;
        }
    }

    static class Producer implements Runnable {
        final BlockingQ<Integer> blockingQ;

        Producer(BlockingQ<Integer> blockingQ) {
            this.blockingQ = blockingQ;
        }

        @Override
        public void run() {
            for (int k=0; k<10; k++) {
                try {
                    blockingQ.put(k);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    static class Consumer implements Runnable {
        final BlockingQ<Integer> blockingQ;

        Consumer(BlockingQ<Integer> blockingQ) {
            this.blockingQ = blockingQ;
        }

        @Override
        public void run() {
            for (int k=0; k<10; k++) {
                try {
                    int item = blockingQ.take();
                    Assert.assertEquals(k, item);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Test
    public void test() throws InterruptedException {
        BlockingQ blockingQ = new BlockingQ(5);
        Producer producer = new Producer(blockingQ);
        Consumer consumer = new Consumer(blockingQ);
        Thread pt = new Thread(producer);
        Thread ct = new Thread(consumer);
        pt.join();
        ct.join();
    }
}
