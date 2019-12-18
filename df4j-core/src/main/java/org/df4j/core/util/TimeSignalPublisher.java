package org.df4j.core.util;

import org.df4j.core.protocol.Completion;
import org.df4j.core.protocol.SignalStream;

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

    public void subscribe(Completion.CompletableObserver subscriber, long delay) {
        TimerTask task = new TimerTask(){
            @Override
            public void run() {
                subscriber.onComplete();
            }
        };
        timer.schedule(task, delay);
    }

    public void subscribe(SignalStream.Subscriber subscriber, long delay) {
        TimerTask task = new TimerTask(){
            @Override
            public void run() {
                subscriber.awake();
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
