package org.df4j.core.messagestream;

import org.df4j.core.boundconnector.messagescalar.MultiScalarInput;
import org.df4j.core.boundconnector.permitscalar.BinarySemafor;
import org.df4j.core.simplenode.messagestream.PickPoint;
import org.df4j.core.tasknode.messagestream.Actor;
import org.junit.Test;

import java.util.Random;
import java.util.Timer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

/**
 * Demonstrates usage of class {@link PickPoint} to model common places for tokens,
 * and actors as handmade coroutines.
 */
public class DiningPhilosophers {
    static final int num = 5; // number of phylosofers
    static int N = 4; // number of rounds

    enum State{Thinking, Hungry1, Hungry2, Eating, Repleted, Died}

    ForkPlace[] forkPlaces = new ForkPlace[num];
    CountDownLatch counter = new CountDownLatch(num);
    Philosopher[] philosophers = new Philosopher[num];
    Timer timer = new Timer();

    //    @Ignore
    @Test
    public void test() throws InterruptedException {
    	// create places for forks with 1 fork in each
        for (int k=0; k < num; k++) {
            ForkPlace forkPlace = new ForkPlace(k);
            forkPlace.post(new Fork(k));
            forkPlaces[k]=forkPlace;
        }
        // create philosophers
        for (int k=0; k<num; k++) {
            philosophers[k] = new Philosopher(k);
        }
        // animate all philosophers
        for (int k=0; k<num; k++) {
            philosophers[k].start();
        }
        assertTrue(counter.await(2, TimeUnit.SECONDS));
    }

    static class Fork  {
        public final String id;

        Fork(int id) {
            this.id = "Fork_"+id;
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
            label="Forkplace_"+id;
        }

        @Override
        public void post(Fork resource) {
 //           System.out.println(label+": put "+resource.toString());
            super.post(resource);
        }
    }

   class Philosopher extends Actor {
        State state = State.Thinking;
        Random rand=new Random();
        int id;
        ForkPlace firstPlace, secondPlace;
        Fork first, second;
        String indent;
        int rounds = 0;

        public Philosopher(int id) {
            this.id = id;
            // to avoid deadlocks, allocate resource with lower number first
            if (id == num-1) {
                firstPlace = forkPlaces[0];
                secondPlace = forkPlaces[id];
            } else {
                firstPlace = forkPlaces[id];
                secondPlace = forkPlaces[id+1];
            }

            StringBuffer sb = new StringBuffer();
            sb.append(id).append(":");
            for (int k = 0; k<id; k++) sb.append("              ");
            indent = sb.toString();
            System.out.println("Ph no. "+id+": first place = "+firstPlace.id+"; second place = "+secondPlace.id+".");
        }

        BinarySemafor signal = new BinarySemafor(this);
        MultiScalarInput<Fork> input = new MultiScalarInput<Fork>(this);

        @Override
        public Void runAction() {
            Thread.currentThread().interrupt();
            loop:
            for (;;) {
                switch(state) {
                    case Thinking:
                        state = State.Hungry1;
                        signal.delay(timer,rand.nextLong() % 17 + 23);
                    case Hungry1:
                        if (!signal.isDone()) {
                            return null;
                        }
                        /**
                         * collects forks one by one
                         */
                        println("Request first (" + firstPlace.id + ")");
                        state = State.Hungry2;
                        input.subscribeTo(firstPlace);
                    case Hungry2:
                        if (!input.isDone()) {
                            return null;
                        }
                        first = input.get();
                        println("Request second (" + secondPlace.id + ")");
                        state = State.Eating;
                        input.subscribeTo(secondPlace);
                    case Eating:
                        if (!input.isDone()) {
                            return null;
                        }
                        second = input.get();
                        state = State.Repleted;
                        signal.delay(timer,rand.nextLong() % 11 + 13);
                    case Repleted:
                        if (!signal.isDone()) {
                            return null;
                        }
                        println("Release first (" + firstPlace.id + ")");
                        firstPlace.post(first);
                        println("Release second (" + secondPlace.id + ")");
                        secondPlace.post(second);
                        rounds++;
                        if (rounds < N) {
                            println("Ph no. "+id+": continue round "+rounds);
                            state = State.Thinking;
                            break;
                        } else {
                            println("Ph no. "+id+": died at round "+rounds);
                            state = State.Died;
                            counter.countDown();
                            stop();
                            return null;
                        }
                }
            }
        }

        private void println(String s) {
            System.out.println(indent+s);
        }

   }

}
