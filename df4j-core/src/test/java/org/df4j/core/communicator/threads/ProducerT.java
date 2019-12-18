package org.df4j.core.communicator.threads;

import org.df4j.core.communicator.AsyncArrayQueue;
import org.df4j.core.util.Utils;

public class ProducerT extends Thread {
    final int delay;
    int cnt;
    AsyncArrayQueue<Integer> queue;

    public ProducerT(int cnt, AsyncArrayQueue<Integer> queue, int delay) {
        this.queue = queue;
        this.delay = delay;
        this.cnt = cnt;
    }

    @Override
    public void run() {
        System.out.println("ProducerT started");
        for (;;) {
            System.out.println("cnt: "+cnt);
            if (cnt == 0) {
                queue.onComplete();
                return;
            }
            try {
                queue.put(cnt);
                cnt--;
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Utils.sneakyThrow(e);
            }
        }
    }
}
