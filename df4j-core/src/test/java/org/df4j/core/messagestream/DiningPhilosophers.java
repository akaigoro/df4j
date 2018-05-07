package org.df4j.core.messagestream;

import org.df4j.core.connector.messagescalar.ScalarInput;
import org.df4j.core.node.AsyncTask;
import org.df4j.core.node.messagestream.PickPoint;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.*;

import static org.junit.Assert.assertTrue;

/**
 * Demonstrates usage of class {@link PickPoint} to model common places for tokens.
 */
public class DiningPhilosophers {
    private static final int num = 5;

    ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);
    Random rand=new Random();
    Executor delayingExecutor = (Runnable next) -> {
        timer.schedule(next, (long)(rand.nextFloat()*50), TimeUnit.MILLISECONDS);
    };

    ForkPlace[] forkPlaces = new ForkPlace[num];

    @Test
    public void test() throws InterruptedException {
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
            philosophers[k] = new Philosopher(k, counter);
        }
        // animate all philosophers
        for (int k=0; k<num; k++) {
            philosophers[k].think();
        }
        assertTrue(counter.await(9000, TimeUnit.MILLISECONDS));
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
        protected void fire() {
            super.fire();
        }

        @Override
        public void postFailure(Throwable t) {
            super.postFailure(t);
        }

        @Override
        public void post(Fork resource) {
            System.out.println(label+": put "+resource.toString());
            super.post(resource);
        }
    }

    class Philosopher {
        int id;
        CountDownLatch counter;
		ForkPlace firstPlace, secondPlace;
        Fork first, second;
        String indent;

        public Philosopher(int id, CountDownLatch counter) {
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

        void think() {
            delayingExecutor.execute(
            new AsyncTask() {
                ScalarInput<Fork> inp = new ScalarInput<>(this);

                @Override
                protected void act() {
                    if (inp.current() == null) {
                        println("Request first (" + firstPlace.id + ")");
                        firstPlace.subscribe(inp);
                    } else if (first == null) {
                        first = inp.current(); inp.purge();
                        println("Request second (" + secondPlace.id + ")");
                        secondPlace.subscribe(inp);
                    } else  {
                        second = inp.current(); inp.purge();
                        eat();
                    }
                }
            });
        }

        int rounds = 0;

        void eat() {
            delayingExecutor.execute(() ->{
                println("Release first (" + firstPlace.id + ")");
                firstPlace.post(first);
                println("Release second (" + secondPlace.id + ")");
                secondPlace.post(second);
                rounds++;
                if (rounds <10) {
                    think();
                } else {
                    counter.countDown();
                }
            });
        }

        private void println(String s) {
            System.out.println(indent+s);
        }
    }
}
