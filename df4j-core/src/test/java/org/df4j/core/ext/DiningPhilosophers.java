package org.df4j.core.ext;

import java.io.PrintStream;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.df4j.core.Actor;
import org.df4j.core.Port;
import org.junit.Test;

/**
 * Demonstrates usage if common places for tokens.
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
    	// create 5 places for sticks with 1 stick in each
        for (int k=0; k<num; k++) {
        	ChopstickPlace stickPlace = new ChopstickPlace();
            stickPlaces[k]=stickPlace;
            stickPlace.postResource(new Chopstick(k));
        }
        // animate all phiosophers
        for (int k=0; k<num; k++) {
        	new Philosopher(k).thinker.start();
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
			reply(inp.value);
		}
		
		protected abstract void reply(Chopstick chopstick);
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

		// think some time
		State thinker = new State(){
			@Override
			protected void act() throws Exception {
				delayedGoTo(firstStick);
			}
		};
	    
		StickRequest firstStick = new StickRequest(){
			@Override
			protected void reply(Chopstick chopstick){
				stickPlaces[leftId].postRequest(secondStick);
				left = chopstick;
				stickPlaces[rightId].postRequest(secondStick);
				secondStick.start();
			}
	    };
	
	    StickRequest secondStick = new StickRequest(){
			@Override
			protected void reply(Chopstick chopstick){
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

	/**
     * represents a stage of a sequential execution process,
     * where stages are active one by one, and explicitly pass
     * control from one to another
     */
    abstract static class State extends Actor {
        Semafor control = new Semafor(0);

        public void start () {
            control.up();
        }
    }
}
