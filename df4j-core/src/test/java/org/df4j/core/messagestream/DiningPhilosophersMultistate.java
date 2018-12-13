package org.df4j.core.messagestream;

import org.df4j.core.boundconnector.messagescalar.ScalarInput;
import org.df4j.core.boundconnector.permitstream.Semafor;
import org.df4j.core.simplenode.messagestream.PickPoint;
import org.df4j.core.tasknode.Action;
import org.df4j.core.tasknode.AsyncAction;
import org.df4j.core.tasknode.messagescalar.AllOf;
import org.df4j.core.tasknode.messagestream.Actor;
import org.df4j.core.util.TimeSignalPublisher;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Demonstrates usage of class {@link AsyncAction} to model finite state machine (FSM).
 * While usual {@link Actor} is an {@link AsyncAction} which restarts itself after each activation
 * and can be considered as a FSM with single state,
 * multistate FSM is modelled as a set or {@link AsyncAction}s which activate each other in predefined order.
 */
public class DiningPhilosophersMultistate {
    static final int num = 5; // number of phylosofers
    static int N = 4; // number of rounds

    ForkPlace[] forkPlaces = new ForkPlace[num];
    Philosopher[] philosophers = new Philosopher[num];
    AllOf listener = new AllOf();
    TimeSignalPublisher timer = new TimeSignalPublisher();

    @Test
    public void test() throws InterruptedException, ExecutionException, TimeoutException {
  //      AsyncProc.setThreadLocalExecutor(AsyncProc.currentThreadExec);
        // create places for forks with 1 fork in each
        for (int k=0; k < num; k++) {
            ForkPlace forkPlace = new ForkPlace(k);
            forkPlace.post(new Fork(k));
            forkPlaces[k]=forkPlace;
        }
        // create philosophers
        for (int k=0; k<num; k++) {
            Philosopher philosopher = new Philosopher(k);
            philosophers[k] = philosopher;
            listener.registerAsyncResult(philosopher);
        }
        // animate all philosophers
        for (int k=0; k<num; k++) {
            philosophers[k].start();
        }
        listener.start();
        listener.get(2, TimeUnit.SECONDS);
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

    /** states:
     * thinking -> hungry(take first fork) -> hungry(take second fork) -> eating -> replete -> thinking
     *
     * state transition is modelled with call to method {@link AsyncAction#start()}
     */
    class Philosopher extends AllOf {
        Random rand = new Random();
        int id;
        ForkPlace firstPlace, secondPlace;
        Fork first, second;
        String indent;
        int rounds = 0;
        final Replete repleted = new Replete();
        final TimeConsuming eating = new TimeConsuming(repleted);
        final Hungry hungry = new Hungry();
        final TimeConsuming thinking = new TimeConsuming(hungry);

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

        public void start() {
            super.start();
            // start state is thinking;
            thinking.start();
        }

        private void println(String s) {
            System.out.println(indent + s);
        }

        /**
         * in order to route possible errors to the enclosing Philosopher instance
         */
        class State extends AsyncAction {
            {
                Philosopher.this.registerAsyncResult(asyncResult());
            }
        }

        /**  at the start, starts timer with random delay
         *   after delay, activates next state
         */
        class TimeConsuming extends State {
            final State nextState;
            Semafor signal = new Semafor(this);

            TimeConsuming(State nextState) {
                this.nextState = nextState;
            }

            @Override
            public synchronized void start() {
                timer.subscribe(signal, rand.nextLong() % 17 + 23);
                super.start();
            }

            @Override
            public Void runAction() {
                nextState.start();
                return null;
            }
        }

        /**
         * at the start, request first fork.
         * after the firs forkt is obtained, requests second fork and reactivates itself.
         * after the second fork is obtained, activates next state.
         */
        class Hungry extends State {
            ScalarInput<Fork> input = new ScalarInput<Fork>(this);

            @Override
            public synchronized void start() {
                println("Request first (" + firstPlace.id + ")");
                firstPlace.subscribe(input);
                super.start();
            }

            @Action
            public void getFork(Fork fork) {
                if (first == null) {
                    first = fork;
                    println("Request second (" + secondPlace.id + ")");
                    secondPlace.subscribe(input);
                    super.start(); // not this.start()
                } else {
                    second = fork;
                    eating.start();
                }
            }
        }

        class Replete extends State {

            @Override
            public Void runAction() {
                println("Release first (" + firstPlace.id + ")");
                firstPlace.post(first);
                println("Release second (" + secondPlace.id + ")");
                secondPlace.post(second);
                first = second = null;
                rounds++;
                if (rounds < N) {
                    println("Ph no. " + id + ": continue round " + rounds);
                    thinking.start();
                } else {
                    println("Ph no. " + id + ": died at round " + rounds);
                    Philosopher.this.completeResult(null);
                }
                return null;
            }

        }

    }

}
