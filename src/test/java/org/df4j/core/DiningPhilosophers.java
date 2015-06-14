package org.df4j.core;

import java.io.PrintStream;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import org.junit.Test;

public class DiningPhilosophers {
    private static final int num = 5;
    PrintStream out=System.out;
    
    Chopstick[] sticks = new Chopstick[num];
    Timer timer=new Timer();
    Random rand=new Random();

    public  void delayedUp(Actor.Semafor sema) {
        out.println("Pause()");
        timer.schedule(new TimerTask(){
            public void run() {
                out.println("Pause End()");
                sema.up();
            }
         }, (long) (rand.nextFloat()*1000));
    }

    @Test
    public void test() throws InterruptedException {
        for (int k=0; k<num; k++) {
            sticks[k]=new Chopstick(k);
        }
        for (int k=0; k<num; k++) {
            delayedUp(new Philosopher(k).startEat);
        }
        Thread.sleep(3000);
    }

    static class Chopstick extends SharedToken<Chopstick>  {
        int id;
        
        public Chopstick(int k) {
            id=k;
        }

        @Override
        public String toString() {
            return "Chopstick["+id+"]";
        }
        
    }
    
    /**
     * when startEat semafor is up, aquires chopsticks
     */
    class Philosopher extends Actor {
        int id;
        Chopstick left;
        Chopstick right;
        Semafor startEat=new Semafor();
        RequestingInput<Chopstick> takeFirst;
        RequestingInput<Chopstick> takeSecond;
        TakerBack takerBack=new TakerBack();
        
        public Philosopher(int id) {
            this.id = id;
            left=sticks[id];
            right=sticks[(id+1)%num];
            if (id==num-1) {
                takeFirst=new RequestingInput<Chopstick>(right);
                takeSecond=new RequestingInput<Chopstick>(left);
            } else {
                takeFirst=new RequestingInput<Chopstick>(left);
                takeSecond=new RequestingInput<Chopstick>(right);
            }
        }

        /**
         * Chopsticks gathered
         */
        @Override
        protected void act() throws Exception {
            delayedUp(takerBack.endEat); // now eat some time, then think
        }

        /** returns chopsticks
         */
        class TakerBack extends Actor {
            Semafor endEat=new Semafor();

            @Override
            public void act() {
                out.println("takerBacl start ");
                left.ret();
                right.ret();
                delayedUp(startEat); // now think some time, then eat
            }
            
        }
        
    }

}
