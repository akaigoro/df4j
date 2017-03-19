package org.df4j.core.ext;

import java.io.PrintStream;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.df4j.core.ext.Dispatcher;
import org.df4j.core.ext.State;
import org.df4j.core.Port;
import org.junit.Test;

/**
 * Demonstrates usage if requestibg pins.
 */
public class DiningPhilosophers {
    private static final int num = 5;
    PrintStream out=System.out;
    Timer timer=new Timer();
    Random rand=new Random();
    
    ChopstickPlace[] stickPlaces = new ChopstickPlace[num];

    void delayedGoTo (State next) {
    	out.println("Pause()");
		timer.schedule(new TimerTask(){
		    public void run() {
		        out.println("Pause End()");
		        next.start();
		    }
		 }, (long) (rand.nextFloat()*1000));
    }

    @Test
    public void test() throws InterruptedException {
        for (int k=0; k<num; k++) {
        	ChopstickPlace stickPlace = new ChopstickPlace();
            stickPlaces[k]=stickPlace;
            stickPlace.postResource(new Chopstick(k));
        }
        for (int k=0; k<num; k++) {
        	delayedGoTo(new Philosopher(k).thinker);
        }
        Thread.sleep(3000);
    }

    static class Chopstick  {
        int id;
        
        public Chopstick(int k) {
            id=k;
        }

        @Override
        public String toString() {
            return "Chopstick["+id+"]";
        }
        
    }
    
    static class ChopstickPlace extends Dispatcher<Chopstick>  {
    }
    

    abstract class StickRequest extends State implements Port<Chopstick>  {
    	Input<Chopstick> inp = new Input<>();
    	
		@Override
		public void post(Chopstick message) {
			inp.post(message);
		}
		@Override
		protected void act() throws Exception {
			act(inp.value);
		}
		
		protected abstract void act(Chopstick chopstick);
    }

    /**
     * when startEat semafor is up, aquires chopsticks
     */
    class Philosopher {
        int id;
        int leftId, rightId;
        Chopstick left, right;
        
        public Philosopher(int id) {
			this.id = id;
			leftId = id;
			rightId = (id+1)%num;
		}

		State thinker = new State(){
			@Override
			protected void act() throws Exception {
				stickPlaces[leftId].postRequest(firstStick);
				firstStick.start();
			}
		};
	    
		StickRequest firstStick = new StickRequest(){
			@Override
			protected void act(Chopstick chopstick){
				left = chopstick;
				stickPlaces[rightId].postRequest(secondStick);
				secondStick.start();
			}
	    };
	
	    StickRequest secondStick = new StickRequest(){
			@Override
			protected void act(Chopstick chopstick){
				right = chopstick;
				delayedGoTo(eater);
			}
	    };

        State eater = new State(){
			@Override
			protected void act() throws Exception {
				stickPlaces[leftId].postResource(left);
				stickPlaces[rightId].postResource(right);
				delayedGoTo(thinker);
			}
		};
    }
}
