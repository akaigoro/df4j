package org.df4j.core.messagestream;

import org.df4j.core.simplenode.messagestream.PickPoint;
import org.df4j.core.tasknode.messagestream.Actor;
import org.junit.Test;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

/**
 * Synchronous implementation, for reference
 */
public class DiningPhilosophersThreaded {
    static final int num = 5; // number of phylosofers
    static int N = 4; // number of rounds
    ForkPlace[] forkPlaces = new ForkPlace[num];
    CountDownLatch counter = new CountDownLatch(num);
    Philosopher[] philosophers = new Philosopher[num];

    @Test
    public void test() throws InterruptedException {
        // create places for forks with 1 fork in each
        for (int k = 0; k < num; k++) {
            ForkPlace forkPlace = new ForkPlace(k);
            forkPlace.post(new Fork(k));
            forkPlaces[k] = forkPlace;
        }
        // create philosophers
        for (int k = 0; k < num; k++) {
            philosophers[k] = new Philosopher(k);
        }
        // animate all philosophers
        for (int k = 0; k < num; k++) {
            philosophers[k].start();
        }
        assertTrue(counter.await(2, TimeUnit.SECONDS));
    }

    static void delay(long delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static class Fork {
        public final String id;

        Fork(int id) {
            this.id = "Fork_" + id;
        }

        @Override
        public String toString() {
            return id;
        }
    }

    static class ForkPlace extends ArrayBlockingQueue<Fork> {
        int id;
        String label;

        public ForkPlace(int k) {
            super(1);
            id = k;
            label = "Forkplace_" + id;
        }

        public void post(Fork resource) {
            //           System.out.println(label+": put "+resource.toString());
            super.add(resource);
        }

        @Override
        public Fork take() {
            try {
                return super.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    class Philosopher extends Thread {
        Random rand = new Random();
        int id;
        ForkPlace firstPlace, secondPlace;
        Fork first, second;
        String indent;
        int rounds = 0;

        public Philosopher(int id) {
            this.id = id;
            // to avoid deadlocks, allocate resource with lower number first
            if (id == num - 1) {
                firstPlace = forkPlaces[0];
                secondPlace = forkPlaces[id];
            } else {
                firstPlace = forkPlaces[id];
                secondPlace = forkPlaces[id + 1];
            }

            StringBuffer sb = new StringBuffer();
            sb.append(id).append(":");
            for (int k = 0; k < id; k++) sb.append("              ");
            indent = sb.toString();
            System.out.println("Ph no. " + id + ": first place = " + firstPlace.id + "; second place = " + secondPlace.id + ".");
        }

        void println(String s) {
            System.out.println(indent + s);
        }

        @Override
        public void run() {
            for (; ; ) {
                // Thinking
                delay(rand.nextLong() % 17 + 23);
                // Hungry
                println("Request first (" + firstPlace.id + ")");
                first = firstPlace.take();
                println("Request second (" + secondPlace.id + ")");
                second = secondPlace.take();
                // Eating
                delay(rand.nextLong() % 11 + 13);
                // Replete
                println("Release first (" + firstPlace.id + ")");
                firstPlace.post(first);
                println("Release second (" + secondPlace.id + ")");
                secondPlace.post(second);
                // check end of life
                rounds++;
                if (rounds < N) {
                    println("Ph no. " + id + ": continue round " + rounds);
                } else {
                    println("Ph no. " + id + ": died at round " + rounds);
                    counter.countDown();
                    return;
                }
            }
        }
    }
}
