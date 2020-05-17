package org.df4j.core.asyncarrayblockingqueue;

import org.df4j.core.communicator.AsyncArrayBlockingQueue;
import org.df4j.core.dataflow.*;
import org.df4j.core.port.InpScalar;
import org.df4j.core.util.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;

import static org.junit.Assert.assertTrue;

/** 1. Using AsyncArrayBlockingQueue to connect threads and actors.
 *  2. Async Philosofer implemented as multistate actor.
 *
 */
public class DiningPhilosophers extends Dataflow {
    static final int num = 5; // number of philosophers
    static int N = 5; // number of rounds
    ForkPlace[] forkPlaces = new ForkPlace[num];
    CountDownLatch counter = new CountDownLatch(num);
    Activity[] philosophers = new Activity[num];
    Random rand = new Random();

    private synchronized long getDelay() {
        return rand.nextInt(17);
    }

    public void abstractTest(Function<Integer, Activity> create) throws InterruptedException {
        // create places for forks with 1 fork in each
        for (int k = 0; k < num; k++) {
            ForkPlace forkPlace = new ForkPlace(k);
            forkPlace.add("Fork_" + k);
            forkPlaces[k] = forkPlace;
        }
        // create philosophers
        for (int k = 0; k < num; k++) {
            philosophers[k] = create.apply(k);
        }
        // start all the philosophers
        for (int k = 0; k < num; k++) {
            philosophers[k].start();
        }
        boolean fin = counter.await(2, TimeUnit.SECONDS);
        assertTrue(fin);
    }

    @Test
    public void testThread() throws InterruptedException {
        abstractTest((Integer k)-> new PhilosopherThread(k));
    }

    @Test
    public void testAsync() throws InterruptedException {
        abstractTest((Integer k)-> new PhilosopherDF(k));
    }

    @Test
    public void testMix() throws InterruptedException {
        abstractTest((Integer k)-> k%2==0? new PhilosopherDF(k): new PhilosopherThread(k));
    }

    static class ForkPlace extends AsyncArrayBlockingQueue<String> {
        int id;
        String label;

        public ForkPlace(int k) {
            super(1);
            id = k;
            label = "Forkplace_" + id;
        }

        public String get() {
            try {
                return super.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Synchronous implementation, for reference
     */
    class PhilosopherThread extends Thread implements ActivityThread {
        protected final Logger logger = new Logger(this);
        int id;
        ForkPlace firstPlace, secondPlace;
        String first, second;
        String indent;
        int rounds = 0;

        public PhilosopherThread(int id) {
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
            logger.info("Ph no. " + id + " (thread): first place = " + firstPlace.id + "; second place = " + secondPlace.id + ".");
        }

        void println(String s) {
            logger.info(indent + s);
        }

        void delay(long delay) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void run() {
            for (; ; ) {
                // Thinking
                delay(getDelay());
                // Hungry
                println("Request first (" + firstPlace.id + ")");
                first = firstPlace.get();
                println("got first "+first + " from "+ firstPlace.id);
                println("Request second (" + secondPlace.id + ")");
                second = secondPlace.get();
                println("got second "+second + " from "+ secondPlace.id);
                // Eating
                delay(getDelay());
                // Replete
                println("Release first "+first +" to " + firstPlace.id);
                firstPlace.add(first);
                println("Release second "+second +" to " + secondPlace.id);
                secondPlace.add(second);
                // check end of life
                rounds++;
                if (rounds == N) {
                    break;
                }
                println("Ph no. " + id + ": continues round " + rounds);
            }
            println("Ph no. " + id + ": died at round " + rounds);
            counter.countDown();
        }
    }

    /**
     * Multistate actor
     */
    class PhilosopherDF extends Actor {
        protected final Logger logger = new Logger(this, Level.INFO);
        InpScalar<String> forkInput;
        int id;
        ForkPlace firstPlace, secondPlace;
        String first, second;
        String indent;
        int rounds = 0;

        public PhilosopherDF(int id) {
            super(DiningPhilosophers.this);
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
            logger.info("Ph no. " + id + " (dataflow): first place = " + firstPlace.id + "; second place = " + secondPlace.id + ".");
        }

        void println(String s) {
            logger.info(indent + s);
        }

        @Override
        protected void runAction() {
            forkInput = new InpScalar<>(this);
            startThinking();
        }

        void startThinking() {
            nextAction(this::endThinking, PORTS_NONE);
            delay(getDelay());
        }

        void endThinking()  {
            println("Request first (" + firstPlace.id + ")");
            firstPlace.subscribe(forkInput);
            nextAction(this::getFork1RequestFork2, forkInput);
        }

        void getFork1RequestFork2()  {
            first = forkInput.remove();
            Assert.assertNotNull(first);
            println("got first "+first + " from "+ firstPlace.id);
            println("Request second (" + secondPlace.id + ")");
            secondPlace.subscribe(forkInput);
            nextAction(this::startEating);
        }

        void startEating() {
            second = forkInput.current();
            Assert.assertNotNull(second);
            println("got second "+second + " from "+ secondPlace.id);
            nextAction(this::endEating, PORTS_NONE);
            delay(getDelay());
        }

        void endEating() {
            println("Release first "+first +" to " + firstPlace.id);
            firstPlace.add(first);
            println("Release second " + second + " to " + secondPlace.id);
            secondPlace.add(second);
            // check end of life
            rounds++;
            if (rounds < N) {
                println("Ph no. " + id + ": continues round " + rounds);
                startThinking();
            } else {
                println("Ph no. " + id + ": died at round " + rounds);
                counter.countDown();
                DiningPhilosophers.this.leave(this);
                complete();
            }
        }
    }
}
