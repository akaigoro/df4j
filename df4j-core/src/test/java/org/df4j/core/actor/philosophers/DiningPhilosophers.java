package org.df4j.core.actor.philosophers;

import org.df4j.core.actor.LazyActor;
import org.df4j.core.actor.ext.PickPoint;
import org.df4j.core.asyncproc.AllOf;
import org.df4j.core.util.TimeSignalPublisher;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertTrue;

/**
 * Demonstrates how coroutines can be emulated.
 */
public class DiningPhilosophers {
    static final int num = 5; // number of phylosophers
    static int N = 4; // number of rounds
    ForkPlace[] forkPlaces = new ForkPlace[num];
    Philosopher[] philosophers = new Philosopher[num];
    TimeSignalPublisher timer = new TimeSignalPublisher();

    @Test
    public void test() throws InterruptedException, TimeoutException, ExecutionException {
        AllOf all = new AllOf();
        // create places for forks with 1 fork in each
        for (int k = 0; k < num; k++) {
            ForkPlace forkPlace = new ForkPlace(k);
            forkPlace.onNext(new Fork(k));
            forkPlaces[k] = forkPlace;
            all.registerAsyncDaemon(forkPlace.asyncResult());
        }
        // create philosophers
        for (int k = 0; k < num; k++) {
            Philosopher philosopher = new Philosopher(k);
            philosophers[k] = philosopher;
            all.registerAsyncResult(philosopher.asyncResult());
        }
        // animate all philosophers
        for (int k = 0; k < num; k++) {
            philosophers[k].startThinking();
        }
        all.asyncResult().get(400, TimeUnit.MILLISECONDS);
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

    class Philosopher extends LazyActor {
        State state;
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

        public void startThinking() {
            state = State.Thinking;
            start();
        }

        public void endThinking() {
            state = State.Hungry1;
            start();
        }

        public void post1(Fork fork) {
            println("Got first (" + firstPlace.id + ")");
            first = fork;
            state = State.Hungry2;
            start();
        }

        public void post2(Fork fork) {
            println("Got second (" + secondPlace.id + ")");
            second = fork;
            state = State.Eating;
            start();
        }

        public void endEating() {
            println("Ended eating");
            state = State.Replete;
            start();
        }

        @Override
        public void runAction() {
            switch (state) {
                case Thinking:
                    println("Started thinking");
                    timer.subscribe(this::endThinking, rand.nextLong() % 17 + 23);
                    return;
                case Hungry1:
                    println("Ended thinking, looking for forks");
                    /**
                     * collect forks one by one
                     */
                    firstPlace.subscribe(this::post1);
                    return;
                case Hungry2:
                    secondPlace.subscribe(this::post2);
                    return;
                case Eating:
                    println("Started eating");
                    timer.subscribe(this::endEating, rand.nextLong() % 11 + 13);
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
                        startThinking();
                    } else {
                        println("Ph no. " + id + ": died at round " + rounds);
                        state = State.Died;
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
