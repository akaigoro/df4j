package org.df4j.core.util;

import org.df4j.core.boundconnector.permitstream.PermitSubscriber;

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

    public void subscribe(PermitSubscriber sema, long delay) {
        Event task = new Event(sema);
        timer.schedule(task, delay);
    }

    private static class Event extends TimerTask {
        private final PermitSubscriber sema;

        public Event(PermitSubscriber sema) {
            this.sema = sema;
        }

        @Override
        public void run() {
            sema.release();
        }
    }
}
