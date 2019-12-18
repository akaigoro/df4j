package org.df4j.core.dataflow;

import org.df4j.core.actor.Activity;
import org.df4j.core.actor.ActivityThread;
import org.df4j.core.actor.Dataflow;
import org.df4j.core.communicator.AsyncArrayQueue;
import org.df4j.core.port.InpMessage;
import org.df4j.core.port.OutChannel;
import org.df4j.core.util.TimeSignalPublisher;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.junit.Assert.assertTrue;

/**
 * Synchronous implementation, for reference
 */
public class DiningPhilosophers {
    static final int num = 5; // number of phylosophers
    static int N = 7; // number of rounds
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
        boolean fin = counter.await(4, TimeUnit.SECONDS);
        asyncPhilosophers.blockingAwait(0);
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

    static class ForkPlace extends AsyncArrayQueue<String> {
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
        TimeSignalPublisher timer = new TimeSignalPublisher();

        StartThinking startThinking = new StartThinking();
        // thinking takes time
        EndThinking endThinking = new EndThinking();
        // to start eating, philosopher need forks
        StartEating startEating = new StartEating();
        // eating takes time
        EndEating endEating = new EndEating();

        public PhilosopherDF(int id) {
            super(asyncPhilosophers);
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
        }

        @Override
        public void start() {
            startThinking.awake();
        }

        void println(String s) {
            System.out.println(indent + s);
        }

        abstract class BasicBlock extends org.df4j.core.actor.BasicBlock {

            protected BasicBlock() {
                super(PhilosopherDF.this);
            }
        }
        class StartThinking extends BasicBlock {

            @Override
            protected void runAction() throws Throwable {
                timer.subscribe(endThinking, getDelay());
            }
        }
        class EndThinking extends BasicBlock {

            @Override
            protected void runAction() throws Throwable {
                startEating.start();
            }
        }
        class StartEating extends BasicBlock {
            InpMessage<String> leftFork = new InpMessage(this);
            InpMessage<String> rightFork = new InpMessage(this);

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
                timer.subscribe(endEating, getDelay());
            }
        }
        class EndEating extends BasicBlock {
            OutChannel<String> leftFork = new OutChannel<>(this);
            OutChannel<String> rightFork = new OutChannel<>(this);

            @Override
            protected void runAction() throws Throwable {
                println("Release left "+left +" to " + leftPlace.id);
                leftFork.onNext(left, leftPlace);
                println("Release right " + right + " to " + rightPlace.id);
                rightFork.onNext(right, rightPlace);
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
/*
Ph no. 0 (dataflow): left place = 0; right place = 1.
Ph no. 1 (thread): left place = 1; right place = 2.
Ph no. 2 (dataflow): left place = 2; right place = 3.
Ph no. 3 (thread): left place = 3; right place = 4.
Ph no. 4 (dataflow): left place = 0; right place = 4.
3:                                          Request left (3)
3:                                          got left Fork_3 from 3
3:                                          Request right (4)
0:Request left (0)
3:                                          got right Fork_4 from 4
0:Request right (1)
4:                                                        Request left (0)
0:got left Fork_0 from 0
4:                                                        Request right (4)
0:got right Fork_1 from 1
2:                            Request left (2)
2:                            Request right (3)
3:                                          Release left Fork_3 to 3
3:                                          Release right Fork_4 to 4
2:                            got left Fork_2 from 2
3:                                          Ph no. 3: continues round 1
2:                            got right Fork_3 from 3
0:Release left Fork_0 to 0
0:Release right Fork_1 to 1
4:                                                        got left Fork_0 from 0
0:Ph no. 0: continues round 1
4:                                                        got right Fork_4 from 4
1:              Request left (1)
1:              got left Fork_1 from 1
1:              Request right (2)
2:                            Release left Fork_2 to 2
2:                            Release right Fork_3 to 3
2:                            Ph no. 2: continues round 1
0:Request left (0)
0:Request right (1)
4:                                                        Release left Fork_0 to 0
4:                                                        Release right Fork_4 to 4
4:                                                        Ph no. 4: continues round 1
3:                                          Request left (3)
3:                                          got left Fork_3 from 3
2:                            Request left (2)
3:                                          Request right (4)
3:                                          got right Fork_4 from 4
2:                            Request right (3)
3:                                          Release left Fork_3 to 3
3:                                          Release right Fork_4 to 4
3:                                          Ph no. 3: continues round 2
2:                            got left Fork_2 from 2
2:                            got right Fork_3 from 3
2:                            Release left Fork_2 to 2
2:                            Release right Fork_3 to 3
2:                            Ph no. 2: continues round 2
4:                                                        Request left (0)
4:                                                        Request right (4)
3:                                          Request left (3)
3:                                          got left Fork_3 from 3
3:                                          Request right (4)
2:                            Request left (2)
2:                            Request right (3)

java.lang.AssertionError
	at org.junit.Assert.fail(Assert.java:86)
	at org.junit.Assert.assertTrue(Assert.java:41)
	at org.junit.Assert.assertTrue(Assert.java:52)
	at org.df4j.core.actor.DiningPhilosophers.abstractTest(DiningPhilosophers.java:55)
	at org.df4j.core.actor.DiningPhilosophers.testMix(DiningPhilosophers.java:70)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.base/java.lang.reflect.Method.invoke(Method.java:567)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:47)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:44)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:271)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:70)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:50)
	at org.junit.runners.ParentRunner$3.run(ParentRunner.java:238)
	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:63)
	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:236)
	at org.junit.runners.ParentRunner.access$000(ParentRunner.java:53)
	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:229)
	at org.junit.runners.ParentRunner.run(ParentRunner.java:309)
	at org.junit.runner.JUnitCore.run(JUnitCore.java:160)
	at com.intellij.junit4.JUnit4IdeaTestRunner.startRunnerWithArgs(JUnit4IdeaTestRunner.java:68)
	at com.intellij.rt.junit.IdeaTestRunner$Repeater.startRunnerWithArgs(IdeaTestRunner.java:33)
	at com.intellij.rt.junit.JUnitStarter.prepareStreamsAndStart(JUnitStarter.java:230)
	at com.intellij.rt.junit.JUnitStarter.main(JUnitStarter.java:58)

*/
