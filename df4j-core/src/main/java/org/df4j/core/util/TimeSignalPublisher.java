package org.df4j.core.util;

import org.df4j.core.protocols.SignalStream;

import java.util.Timer;
import java.util.TimerTask;

public class TimeSignalPublisher {
    protected final Timer timer;

    public TimeSignalPublisher(Timer timer) {
        this.timer = timer;
    }

    public TimeSignalPublisher() {
        this(new Timer());
    }

    public void subscribe(SignalStream.Subscriber sema, long delay) {
        TimerTask task = new TimerTask(){
            @Override
            public void run() {
                sema.release();
            }
        };
        timer.schedule(task, delay);
    }

    public void subscribe(Runnable taskBody, long delay) {
        TimerTask task = new TimerTask(){
            @Override
            public void run() {
                taskBody.run();
            }
        };
        timer.schedule(task, delay);
    }
}
