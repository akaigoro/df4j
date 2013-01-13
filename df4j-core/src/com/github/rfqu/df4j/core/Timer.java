package com.github.rfqu.df4j.core;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.github.rfqu.df4j.core.DFContext.ContextThreadFactory;
import com.github.rfqu.df4j.core.DFContext.ItemKey;


public class Timer {
	private  ScheduledThreadPoolExecutor timerThread;
    
	private Timer(DFContext context) {
		ContextThreadFactory tf = context.new ContextThreadFactory(" DF Timer ");
        timerThread=new ScheduledThreadPoolExecutor(1, tf);
    }


    public  <T> void scheduleAt(Port<T> port, T message, long timeToFire) {
        schedule(port, message, timeToFire-System.currentTimeMillis());
    }
    
    public  <T> void schedule(Port<T> port, T message, long delay) {
        TimerTask<T> command=new TimerTask<T>(port, message, System.currentTimeMillis()+delay);
        timerThread.schedule(command, delay, TimeUnit.MILLISECONDS);
    }
    
    public  <T> void schedule(Runnable task, long delay) {
        timerThread.schedule(task, delay, TimeUnit.MILLISECONDS);
    }
    
    public  <T> void scheduleAt(Runnable task, long timeToFire) {
        timerThread.schedule(task, timeToFire-System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }
    
    public CallbackFuture<Void> shutdown() {
        timerKey.remove();
        timerThread.shutdown();
        // wait full timer termination after shutdown
        return new CallbackFuture<Void>(){
		    @Override
		    public synchronized Void get() throws InterruptedException {
		        timerThread.awaitTermination(1000, TimeUnit.DAYS);
		        return null;
		    }
		};
   }

	public void cancel() {
		timerThread.shutdownNow();
	}

	static private final class TimerTask<T> implements Runnable, Comparable<TimerTask<T>>{
		Port<T> port;
		T message;
		long timeToFire;
		
		public TimerTask(Port<T> port, T message, long timeToFire) {
			this.port = port;
			this.message = message;
			this.timeToFire = timeToFire;
		}

		public void run() {
			port.post(message);
		}

		@Override
		public int compareTo(TimerTask<T> o) {
			if  (timeToFire<o.timeToFire) return -1;
			if  (timeToFire>o.timeToFire) return +1;
			return 0;
		}
	}

	//--------------------- context
	
	private static ItemKey<Timer> timerKey=DFContext.getCurrentContext().new ItemKey<Timer>() {

        @Override
        protected Timer initialValue(DFContext context) {
            return new Timer(context);
        }
	    
	};
    
    public static Timer getCurrentTimer() {
        return timerKey.get();
    }
}
