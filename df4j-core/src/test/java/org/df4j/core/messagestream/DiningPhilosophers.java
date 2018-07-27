package org.df4j.core.messagestream;

import org.df4j.core.boundconnector.messagescalar.ScalarInput;
import org.df4j.core.tasknode.Action;
import org.df4j.core.tasknode.AsyncAction;
import org.df4j.core.tasknode.AsyncProc;
import org.df4j.core.tasknode.messagestream.Actor;
import org.df4j.core.simplenode.messagestream.PickPoint;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

/**
 * Demonstrates usage of class {@link PickPoint} to model common places for tokens.
 */
public class DiningPhilosophers {
    private static final int num = 5;

//    @Ignore
    @Test
    public void test() throws InterruptedException {
        ForkPlace[] forkPlaces = new ForkPlace[num];
        CountDownLatch counter = new CountDownLatch(num);
        Philosopher[] philosophers = new Philosopher[num];
    	// create places for forks with 1 fork in each
        for (int k=0; k < num; k++) {
            ForkPlace forkPlace = new ForkPlace(k);
            forkPlace.post(new Fork(k));
            forkPlaces[k]=forkPlace;
        }
        // create philosophers
        for (int k=0; k<num; k++) {
            philosophers[k] = new Philosopher(k, forkPlaces, counter);
        }
        // animate all philosophers
        for (int k=0; k<num; k++) {
            philosophers[k].start();
        }
        assertTrue(counter.await(2000, TimeUnit.MILLISECONDS));
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
            System.out.println(label+": put "+resource.toString());
            super.post(resource);
        }
    }

    /**
     * while ordinary {@link Actor} is a single {@link AsyncProc}
     * which restarts itself,
     * this class comprises of several {@link AsyncProc}s which activate each other cyclically.
     */
    static class Philosopher {
        Random rand=new Random();
        int id;
        CountDownLatch counter;
		ForkPlace firstPlace, secondPlace;
        Fork first, second;
        String indent;
        int rounds = 0;

        public Philosopher(int id, ForkPlace[] forkPlaces, CountDownLatch counter) {
            this.id = id;
            this.counter = counter;
            // to avoid deadlocks, allocate resource with lower number first
            if (id == num-1) {
                firstPlace = forkPlaces[0];
                secondPlace= forkPlaces[id];
            } else {
                firstPlace = forkPlaces[id];
                secondPlace = forkPlaces[id+1];
            }

            StringBuffer sb = new StringBuffer();
            sb.append(id).append(":");
            for (int k = 0; k<=id; k++) sb.append("  ");
            indent = sb.toString();
            println("first place ("+firstPlace.id+") second place ("+secondPlace.id+")");
        }

        Hungry hungry = new Hungry();
        Replete replete = new Replete();
        AsyncAction think = new DelayedAsyncProc(hungry);
        AsyncAction eat = new DelayedAsyncProc(replete);

        public void start() {
            think.start();
        }

        private void println(String s) {
            System.out.println(indent+s);
        }

        private class DelayedAsyncProc extends AsyncAction<Void> {
            final AsyncAction next;

            private DelayedAsyncProc(AsyncAction next) {
                this.next = next;
            }

            @Action
            protected Void act() throws InterruptedException {
                Thread.sleep(rand.nextLong()%11+11);
                next.start();
                return null;
            }
        }

        /**
         * collects forks one by one
         */
        private class Hungry extends AsyncAction<Void> {
            ScalarInput<Fork> input = new ScalarInput<>(this);

            @Override
            public void start() {
                println("Request first (" + firstPlace.id + ")");
                firstPlace.subscribe(this.input);
                super.start();
            }

            @Action
            protected void act(Fork fork) {
                if (first == null) {
                    first = fork;
                    println("Request second (" + secondPlace.id + ")");
                    secondPlace.subscribe(this.input);
                    super.start();
                } else  {
                    second = fork;
                    eat.start();
                }
            }
        }

        /** return forks
         *
         */
        private class Replete extends AsyncAction<Void> {

            @Action
            protected Void act() {
                println("Release first (" + firstPlace.id + ")");
                firstPlace.post(first);
                println("Release second (" + secondPlace.id + ")");
                secondPlace.post(second);
                rounds++;
                if (rounds < 10) {
                    think.start();
                } else {
                    counter.countDown();
                }
                return null;
            }
        }
    }
}
