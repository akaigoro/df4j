package org.df4j.core.boundconnector.permitscalar;

import org.df4j.core.tasknode.AsyncProc;

import java.util.Timer;
import java.util.TimerTask;

/**
 * blocks the parent async procedure only when subscribed
 *
 */
public class BinarySemafor extends AsyncProc.Lock {
    private AsyncProc task;

    public BinarySemafor(AsyncProc task) {
        task.super(false);
    }

    public boolean isDone() {
        return !isBlocked();
    }

    public void aquire() {
        super.turnOff();
    }

    public void release() {
        turnOn();
    }

    public void delayedRelease(Timer timer, long delay) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                release();
            }
        }, delay);
    }

    public void delay(Timer timer, long delay) {
        aquire();
        delayedRelease(timer, delay);
    }
}
