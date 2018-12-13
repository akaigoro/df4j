package org.df4j.core.util.asyncmon;

import org.df4j.core.simplenode.messagescalar.CompletablePromise;
import org.df4j.core.tasknode.messagestream.Actor1;

import java.util.ArrayDeque;
import java.util.Queue;

public class AsyncObject {
    private MonitorImpl monitor = new MonitorImpl();

    public void exec(CriticalSection task) {
        monitor.post(task);
    }

    protected CompletablePromise<?> asyncResult() {
        return monitor.asyncResult();
    }

    static class MonitorImpl extends Actor1<CriticalSection> implements AsyncMonitor {
        Queue<CriticalSection> waiting = new ArrayDeque();
        boolean wantsToWait = false;

        {
            start();
        }

        public void doWait() {
            wantsToWait = true;
        }

        public void doNotify() {
            CriticalSection task = waiting.poll();
            if (task != null) {
                super.post(task);
            }
        }

        public void doNotifyAll() {
            for (;;) {
                CriticalSection task = waiting.poll();
                if (task == null) {
                    return;
                }
                super.post(task);
            }
        }

        @Override
        protected void runAction(CriticalSection task) throws Exception {
            task.act(this);
            if (wantsToWait) {
                wantsToWait = false;
                waiting.add(task);
            }
        }

        @Override
        protected void fire() {
            if (!mainInput.hasNext()) {
                throw new IllegalStateException();
            }
            super.fire();
        }
    }
}
