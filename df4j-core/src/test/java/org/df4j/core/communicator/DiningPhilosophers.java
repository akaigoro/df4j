package org.df4j.core.communicator;

import org.df4j.core.dataflow.Activity;
import org.df4j.core.dataflow.ActivityThread;
import org.df4j.core.dataflow.Dataflow;
import org.df4j.core.port.InpScalar;
import org.df4j.core.port.OutChannel;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.Assert.assertTrue;

/**
 * Synchronous implementation, for reference
 */
public class DiningPhilosophers extends Dataflow {
    static final int num = 2; // number of phylosophers
    static int N = 3; // number of rounds
    ForkPlace[] forkPlaces = new ForkPlace[num];
    CountDownLatch counter = new CountDownLatch(num);
    Activity[] philosophers = new Activity[num];
    Dataflow asyncPhilosophers = new Dataflow();
    Random rand = new Random();

    private synchronized long getDelay() {
        return rand.nextInt(17);
    }

    public void abstractTest(Function<Integer, Activity> create) throws InterruptedException {
        // create places for forks with 1 fork in each
        for (int k = 0; k < num; k++) {
            ForkPlace forkPlace = new ForkPlace(k);
            forkPlace.put("Fork_" + k);
            forkPlaces[k] = forkPlace;
        }
        // create philosophers
        for (int k = 0; k < num; k++) {
            philosophers[k] = create.apply(k);
        }
        // animate all philosophers
        for (int k = 0; k < num; k++) {
            philosophers[k].start();
        }
        boolean fin = counter.await(2, TimeUnit.SECONDS);
        super.blockingAwait(0);
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

        @Override
        public void put(String fork) {
            try {
                super.put(fork);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public String get() {
            try {
                return super.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    class PhilosopherThread extends Thread implements ActivityThread {
        int id;
        ForkPlace leftPlace, rightPlace;
        String left, right;
        String indent;
        int rounds = 0;

        public PhilosopherThread(int id) {
            this.id = id;
            // to avoid deadlocks, allocate resource with lower number first
            if (id == num - 1) {
                leftPlace = forkPlaces[0];
                rightPlace = forkPlaces[id];
            } else {
                leftPlace = forkPlaces[id];
                rightPlace = forkPlaces[id + 1];
            }

            StringBuffer sb = new StringBuffer();
            sb.append(id).append(":");
            for (int k = 0; k < id; k++) sb.append("              ");
            indent = sb.toString();
            System.out.println("Ph no. " + id + " (thread): left place = " + leftPlace.id + "; right place = " + rightPlace.id + ".");
        }

        void println(String s) {
            System.out.println(indent + s);
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
                println("Request left (" + leftPlace.id + ")");
                left = leftPlace.get();
                println("got left "+left + " from "+ leftPlace.id);
                println("Request right (" + rightPlace.id + ")");
                right = rightPlace.get();
                println("got right "+right + " from "+ rightPlace.id);
                // Eating
                delay(getDelay());
                // Replete
                println("Release left "+left +" to " + leftPlace.id);
                leftPlace.put(left);
                println("Release right "+right +" to " + rightPlace.id);
                rightPlace.put(right);
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

    class PhilosopherDF extends Dataflow {
        int id;
        ForkPlace leftPlace, rightPlace;
        String left, right;
        String indent;
        int rounds = 0;

        StartThinking startThinking;
        // thinking takes time
        EndThinking endThinking;
        // to start eating, philosopher need forks
        StartEating startEating;
        // eating takes time
        EndEating endEating;

        public PhilosopherDF(int id) {
            super(DiningPhilosophers.this);
            this.id = id;
            // to avoid deadlocks, allocate resource with lower number first
            if (id == num - 1) {
                leftPlace = forkPlaces[0];
                rightPlace = forkPlaces[id];
            } else {
                leftPlace = forkPlaces[id];
                rightPlace = forkPlaces[id + 1];
            }

            StringBuffer sb = new StringBuffer();
            sb.append(id).append(":");
            for (int k = 0; k < id; k++) sb.append("              ");
            indent = sb.toString();
            System.out.println("Ph no. " + id + " (dataflow): left place = " + leftPlace.id + "; right place = " + rightPlace.id + ".");

            startThinking = new StartThinking();
            endThinking = new EndThinking();
            startEating = new StartEating();
            endEating = new EndEating();
        }

        @Override
        public void start() {
            startThinking.awake();
        }

        void println(String s) {
            System.out.println(indent + s);
        }

        abstract class BasicBlock extends org.df4j.core.dataflow.BasicBlock {

            protected BasicBlock() {
                super(PhilosopherDF.this);
            }
        }

        class StartThinking extends BasicBlock {

            @Override
            protected void runAction() throws Throwable {
                endThinking.awake(getDelay());
            }
        }

        class EndThinking extends BasicBlock {

            @Override
            protected void runAction() throws Throwable {
                startEating.start();
            }
        }

        class StartEating extends BasicBlock {
            InpScalar<String> leftFork = new InpScalar<>(this);
            InpScalar<String> rightFork = new InpScalar<>(this);

            void start() {
                println("Request left (" + leftPlace.id + ")");
                leftPlace.subscribe(leftFork);
                println("Request right (" + rightPlace.id + ")");
                rightPlace.subscribe(rightFork);
                awake();
            }

            @Override
            protected void runAction() throws Throwable {
                left = leftFork.current();
                println("got left "+left + " from "+ leftPlace.id);
                right = rightFork.current();
                println("got right "+right + " from "+ rightPlace.id);
                endEating.awake(getDelay());
            }
        }
        class EndEating extends BasicBlock {
            OutChannel<String> leftFork = new OutChannel<>(this);
            OutChannel<String> rightFork = new OutChannel<>(this);
            {
                leftPlace.subscribe(leftFork);
                rightPlace.subscribe(rightFork);
            }

            @Override
            protected void runAction() throws Throwable {
                println("Release left "+left +" to " + leftPlace.id);
                leftFork.onNext(left);
                println("Release right " + right + " to " + rightPlace.id);
                rightFork.onNext(right);
                // check end of life
                rounds++;
                if (rounds < N) {
                    println("Ph no. " + id + ": continues round " + rounds);
                    startThinking.awake();
                } else {
                    println("Ph no. " + id + ": died at round " + rounds);
                    counter.countDown();
                }
            }
        }
    }
}
