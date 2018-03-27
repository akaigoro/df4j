package org.df4j.core.ext;

import org.df4j.core.Actor;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates usage of class {@link Dispatcher} to model common places for tokens.
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
        Philosopher[] philosophers = new Philosopher[num];
    	// create places for forks with 1 fork in each
        for (int k=0; k<num; k++) {
            ForkPlace forkPlace = new ForkPlace(k);
            forkPlace.postResource(new Fork());
            forkPlaces[k]=forkPlace;
        }
        // create philosophers
        for (int k=0; k<num; k++) {
            philosophers[k] = new Philosopher(k);
        }
        // animate all philosophers
        for (int k=0; k<num; k++) {
            philosophers[k].think.start();
        }
        Thread.sleep(300);
    }

    static class Fork  { }
    
    static class ForkPlace extends Dispatcher<Fork>  {
        int id;

        public ForkPlace(int k) {
            id = k;
        }
    }

    abstract class Stage extends Actor {
        Semafor canRun = new Semafor();

        {
            setExecutor(delayingExecutor);
        }

        public void start() {
            canRun.release(1);
        }
    }

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
            sb.append(id).append(":");
            for (int k = 0; k<=id; k++) sb.append("  ");
            indent = sb.toString();
            println("first place ("+firstPlace.id+") second place ("+secondPlace.id+")");
        }

        Stage think = new Stage(){
            @Override
            protected void act() {
                println("Request first ("+firstPlace.id+")");
                firstPlace.postRequest(hungry1);
            }
        };

        Actor1<Fork> hungry1 = new Actor1<Fork>() {
            @Override
            protected void act(Fork fork) {
                println("Got first ("+firstPlace.id+"), request second ("+secondPlace.id+")");
                first = fork;
                secondPlace.postRequest(hungry2);
            }
        };

        Actor1<Fork> hungry2 = new Actor1<Fork>() {
            @Override
            protected void act(Fork fork) {
                println("Got second ("+secondPlace.id+"), eating");
                second = fork;
                eat.start();
            }
        };

        Stage eat = new Stage() {
            @Override
            protected void act() throws Exception {
                println("Return first ("+firstPlace.id+"), return second ("+secondPlace.id+")");
                firstPlace.postResource(first);
                secondPlace.postResource(second);
                first = second = null;
                think.start();
            }
        };

        private void println(String s) {
            System.out.println(indent+s);
        }
    }
}
