package org.df4j.core.messagestream;

import org.df4j.core.simplenode.messagestream.PickPoint;
import org.df4j.core.tasknode.AsyncAction;
import org.df4j.core.util.TimeSignalPublisher;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

/**
 * Demonstrates how coroutines can be emulated.
 */
public class DiningPhilosophers {
    static final int num = 5; // number of phylosofers
    static int N = 4; // number of rounds
    ForkPlace[] forkPlaces = new ForkPlace[num];
    CountDownLatch counter = new CountDownLatch(num);
    Philosopher[] philosophers = new Philosopher[num];
    TimeSignalPublisher timer = new TimeSignalPublisher();

    @Test
    public void test() throws InterruptedException {
        // create places for forks with 1 fork in each
        for (int k = 0; k < num; k++) {
            ForkPlace forkPlace = new ForkPlace(k);
            forkPlace.onNext(new Fork(k));
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

    enum State {Thinking, Hungry1, Hungry2, Eating, Replete, Died}

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

    static class ForkPlace extends PickPoint<Fork> {
        int id;
        String label;

        public ForkPlace(int k) {
            id = k;
            label = "Forkplace_" + id;
        }
    }

    class Philosopher extends AsyncAction {
        State state = State.Thinking;
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

        public void post1(Fork fork) {
            first = fork;
            start();
        }

        public void post2(Fork fork) {
            second = fork;
            start();
        }

        @Override
        public void runAction() {
            switch (state) {
                case Thinking:
                    state = State.Hungry1;
                    timer.subscribe(this::start, rand.nextLong() % 17 + 23);
                    return;
                case Hungry1:
                    /**
                     * collect forks one by one
                     */
                    println("Request first (" + firstPlace.id + ")");
                    state = State.Hungry2;
                    firstPlace.subscribe(this::post1);
                    return;
                case Hungry2:
                    println("Request second (" + secondPlace.id + ")");
                    state = State.Eating;
                    secondPlace.subscribe(this::post2);
                    return;
                case Eating:
                    state = State.Replete;
                    timer.subscribe(this::start, rand.nextLong() % 11 + 13);
                    return;
                case Replete:
                    println("Release first (" + firstPlace.id + ")");
                    firstPlace.onNext(first);
                    first = null;
                    println("Release second (" + secondPlace.id + ")");
                    secondPlace.onNext(second);
                    second = null;
                    rounds++;
                    if (rounds < N) {
                        println("Ph no. " + id + ": continue round " + rounds);
                        state = State.Thinking;
                        start();
                    } else {
                        println("Ph no. " + id + ": died at round " + rounds);
                        state = State.Died;
                        counter.countDown();
                        stop();
                    }
                    return;
                default:
                    throw new IllegalStateException();
            }
        }

        private void println(String s) {
            System.out.println(indent + s);
        }
    }
}
