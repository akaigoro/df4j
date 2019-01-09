package org.df4j.core.util;

import org.df4j.core.boundconnector.permitscalar.ScalarPermitSubscriber;

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

    public void subscribe(ScalarPermitSubscriber sema, long delay) {
        Event task = new Event(sema);
        timer.schedule(task, delay);
    }

    private static class Event extends TimerTask {
        private final ScalarPermitSubscriber sema;

        public Event(ScalarPermitSubscriber sema) {
            this.sema = sema;
        }

        @Override
        public void run() {
            sema.release();
        }
    }
}
