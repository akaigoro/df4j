package com.github.rfqu.df4j.core;

import java.util.TimerTask;

public class Timer {
    private static java.util.Timer timer;
    
    private static synchronized java.util.Timer getTimer() {
        if (timer==null) {
            timer=new java.util.Timer();
        }
        return timer;
    }
    
    public static void schedule(final Task task, long delay) {
        TimerTask ttask=new TimerTask() {
            @Override
            public void run() {
                task.fire();
            }
        };
        getTimer().schedule(ttask, delay);
    }
    
    public static <T> void schedule(final Port<T> port, final T message, long delay) {
        TimerTask ttask=new TimerTask() {
            @Override
            public void run() {
                port.send(message);
            }
        };
        getTimer().schedule(ttask, delay);
    }

    public static void shutdown() {
        java.util.Timer timerloc=null;
        synchronized (Timer.class) {
            timerloc=timer;
            if (timerloc!=null) {
                timer=null;
            }        
        }
        if (timerloc!=null) {
            timerloc.cancel();
        }        
    }
    
}
