package org.df4j.core.messagestream;

import org.df4j.core.boundconnector.messagescalar.*;
import org.df4j.core.tasknode.AsyncAction;
import org.df4j.core.simplenode.messagestream.PickPoint;
import org.df4j.core.tasknode.messagestream.Actor;
import org.junit.Test;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

/**
 * Demonstrates usage of class {@link PickPoint} to model common places for tokens.
 */
public class DiningPhilosophers {
    static final int num = 5; // number of phylosofers
    static int N = 1; // nuber of rounds

    enum State{Thinking, Hungry, Hungry1, Eating, Repleted, Died}

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

   class Philosopher extends AsyncAction<Void> {
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
            for (int k = 0; k<=id; k++) sb.append("  ");
            indent = sb.toString();
            println("Ph no. "+id+": first place ("+firstPlace.id+") second place ("+secondPlace.id+")");
        }

        DelayedSignal signal = new DelayedSignal();
        SingleInput<Fork> input = new SingleInput<Fork>();

        @Override
        public Void runAction() {
            loop:
            for (;;) {
                switch(state) {
                    case Thinking:
                        state = State.Hungry;
                        signal.delay(rand.nextLong() % 11 + 11);
                        break loop;
                    case Hungry:
                        /**
                         * collects forks one by one
                         */
                        println("Request first (" + firstPlace.id + ")");
                        input.subscribeTo(firstPlace);
                        state = State.Hungry1;
                    case Hungry1:
                        if (!input.isDone()) {
                            break loop;
                        }
                        first = input.get();
                        println("Request second (" + secondPlace.id + ")");
                        input.subscribeTo(secondPlace);
                        state = State.Eating;
                    case Eating:
                        if (!input.isDone()) {
                            break loop;
                        }
                        second = input.get();
                        state = State.Repleted;
                        signal.delay(rand.nextLong() % 11 + 11);
                        break loop;
                    case Repleted:
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
                            println("Ph no. "+id+": dye at round "+rounds);
                            state = State.Died;
                            counter.countDown();
                            stop();
                            return null;
                        }
                }
            }
            start();
            return null;
        }

        private void println(String s) {
            System.out.println(indent+s);
        }

       protected class DelayedSignal extends Lock {
           boolean done;

           public DelayedSignal() {
               super(false);
           }

           public boolean isDone() {
               return done;
           }

           public void delay(long delay) {
               super.turnOff();
               done = false;
               timer.schedule(new TimerTask() {
                   @Override
                   public void run() {
                       done = true;
                       turnOn();
                   }
               }, delay);
           }
       }

       protected class SingleInput<T> extends Lock implements ScalarSubscriber<T> {
           boolean done;
           T value;

           public SingleInput() {
               super(false);
           }

           public boolean isDone() {
               return done;
           }

           public void subscribeTo(ScalarPublisher publisher) {
               super.turnOff();
               done = false;
               publisher.subscribe(this);
           }

           @Override
           public boolean complete(T message) {
               value = message;
               done = true;
               turnOn();
               return true;
           }

           public T get() {
               return value;
           }
       }
    }
}
