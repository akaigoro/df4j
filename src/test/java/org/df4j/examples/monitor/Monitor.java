package org.df4j.examples.monitor;

import java.util.LinkedList;
import org.df4j.core.Actor1;

/**
 * Demonstrates how such low-level mechanism as java Monitor
 * (with synchronized/wait/notify operators) can be modelled
 * by df4j core classes.
 * Below is a typical flowchart of a synchronized method:
 * <pre>
 * {@code
      v----------------------------------------------<
      |                                              |
/ monitorexit /                                      |    
      |                                              |
      >-->||                                         |
process-->||-- (state ok?): no--> / wait(); / >----->|
                   :yes                              ^
                     |                               |
                     v                               |
                 / change();    /                    |
                 / notifyAll(); /                    |
                 / return;      /                    |
                     |                               |
                     >------------------------------>^
 * }
 * </pre>                    
 *  MonitorEnter and MonitorExit are modelled with the Actor's fire bit.
 *  We may say all Actor's act() methods are synchronized by construction.
 *  Monitor is just an Actor with main input of type Runnable and 
 *  internal queue to store Runnables which want to wait.
 */
class Monitor<M extends Monitor<M>> extends Actor1<Runnable>{
    LinkedList<Runnable> eventQueue=new LinkedList<>();
    Runnable current;
    
    @Override
    protected void act(Runnable action) throws Exception {
        current=action;
        action.run();
        current=null;
    }

    /**
     * place current process to the event queue
     */
    protected void doWait() {
        eventQueue.add(current);
    }

    /**
     * pass one waiting processes to the execution queue
     */
    protected void doNotify() {
        Runnable action=eventQueue.poll();
        if (action!=null) {
            this.post(action); 
        }
    }

    /**
     * pass all waiting processes to the execution queue
     */
    protected void doNotifyAll() {
        for (;;) { 
            Runnable action=eventQueue.poll();
            if (action==null) break;
            this.post(action); 
        }
    }

}