package org.df4j.core.ext;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.df4j.core.Port;
import org.junit.Test;

/**
 * Demonstrates usage of class {@link Dispatcher} to model common places for tokens.
 */
public class DiningPhilosophers {
    private static final int num = 5;

    ScheduledExecutorService timer = Executors.newScheduledThreadPool(2);
    Random rand=new Random();
    ForkPlace[] forkPlaces = new ForkPlace[num];

    void delayedRun(Runnable next) {
		timer.schedule(next, (long) (rand.nextFloat()*100), TimeUnit.MILLISECONDS);
    }

    @Test
    public void test() throws InterruptedException {
        Philosopher[] philosophers = new Philosopher[num];
    	// create places for forks with 1 fork in each
        for (int k=0; k<num; k++) {
            ForkPlace forkPlace = new ForkPlace(k);
            forkPlaces[k]=forkPlace;
            forkPlace.postResource(new Fork(k));
        }
        // create philosophers
        for (int k=0; k<num; k++) {
            philosophers[k] = new Philosopher(k);
        }
        // animate all philosophers
        for (int k=0; k<num; k++) {
            philosophers[k].think();
        }
        Thread.sleep(300);
    }

    static class Fork  {
        int id;
        
        public Fork(int k) {
            id=k;
        }
    }
    
    static class ForkPlace extends Dispatcher<Fork>  {
        int id;

        public ForkPlace(int k) {
            id = k;
        }
    }

    /**
     * think -> requestFirst -> getFirstRequestSecond -> getSecondEat -> returnForks -> think
     */
    class Philosopher {
        int id;
		ForkPlace firstPlace, secondPlace;
        Fork first, second;
        String indent;
        
        public Philosopher(int id) {
			this.id = id;
			// to avoid deadlocks, allocate resource with lower number first
			if (id == num-1) {
                firstPlace = forkPlaces[0];
                secondPlace= forkPlaces[id];
            } else {
                firstPlace = forkPlaces[id];
                secondPlace = forkPlaces[id+1];
            }
            StringBuffer sb = new StringBuffer();
			for (int k = 0; k<id; k++) sb.append("  ");
            indent = sb.toString();
            println("first place ("+firstPlace.id+") second place ("+secondPlace.id+")");
		}

        private void println(String s) {
            System.out.println(indent+s+" /"+id);
        }

		/* states are represented as callbacks, executed on caller's stack
		 * this is safe because chain of calls is short - it breaks when the timer is accessed
		 */

        // think some time, then request the first fork
        void think () {
            println("Thinking");
            delayedRun (requestFirst);
        };

        // request the first fork
        Runnable requestFirst = new Runnable() {
            @Override
            public void run() {
                println("Request first ("+firstPlace.id+")");
                firstPlace.postRequest(getFirstRequestSecond);
            }
        };

        // get the first fork and request the second one
        Port<Fork> getFirstRequestSecond = new Port<Fork>() {
            @Override
            public void post(Fork fork) {
                println("Got first ("+fork.id+"), request second ("+secondPlace.id+")");
                first = fork;
                secondPlace.postRequest(getSecondEat);
            }
        };

        // get the second fork and start eating
        Port<Fork> getSecondEat = new Port<Fork>() {
            @Override
            public void post(Fork fork) {
                println("Got second ("+fork.id+"), eating");
                second = fork;
                delayedRun (returnForks);
            }
        };

        // return both forks in reverse order and start thinking
        Runnable returnForks =new Runnable() {
            @Override
            public void run() {
                println("Return second ("+second.id+"), return first ("+first.id+")");
                secondPlace.postResource(second);
                firstPlace.postResource(first);
                think();
            }
        };
    }
}
