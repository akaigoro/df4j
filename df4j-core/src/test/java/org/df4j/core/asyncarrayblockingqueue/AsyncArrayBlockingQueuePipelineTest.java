package org.df4j.core.asyncarrayblockingqueue;

import org.df4j.core.communicator.AsyncArrayBlockingQueue;
import org.df4j.core.dataflow.Activity;
import org.df4j.core.dataflow.ActivityThread;
import org.df4j.core.dataflow.Actor;
import org.df4j.core.port.InpFlow;
import org.df4j.core.port.OutChannel;
import org.df4j.core.util.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;

public class AsyncArrayBlockingQueuePipelineTest {
    protected final Logger logger = new Logger(this);
    static final int N=20;
    static final int n1=1, n2=3;

    Random rand = new Random();
    private synchronized long getDelay() {
        return rand.nextInt(17);
    }

    public void test2steps(Creator create) throws InterruptedException {
        Activity[] activities = new Activity[n1+n2];
        AsyncArrayBlockingQueue<Integer> queue1 = new AsyncArrayBlockingQueue<Integer>(N);
        AsyncArrayBlockingQueue<Integer> queue2 = new AsyncArrayBlockingQueue<Integer>(N/4);
        AsyncArrayBlockingQueue<Integer> queue3 = new AsyncArrayBlockingQueue<Integer>(N);
        for (int k = 0; k < n1; k++) {
            activities[k] = create.create(k, queue1, queue2);
        }
        for (int k = n1; k < n1+n2; k++) {
            activities[k] = create.create(k, queue2, queue3);
        }
        for (int k = 0; k < n1+n2; k++) {
            activities[k].start();
        }
        for (int k = 0; k < N; k++) {
            queue1.put(new Integer(k));
        }
        boolean[] result = new boolean[N];
        int failCount = 0;
        for (int k = 0; k < N; k++) {
            int res = queue3.take();
            logger.info("got "+res);
            if (result[res]) {
                failCount++;
            }
            result[res] = true;
        }
        for (int k = 0; k < N; k++) {
            if (result[k]==false) {
                failCount++;
            }
        }
        Assert.assertEquals(0, failCount);
    }

    public void test1steps(Creator create) throws InterruptedException {
        Activity[] activities = new Activity[n1];
        AsyncArrayBlockingQueue<Integer> queue1 = new AsyncArrayBlockingQueue<Integer>(N);
        AsyncArrayBlockingQueue<Integer> queue2 = new AsyncArrayBlockingQueue<Integer>(N/4);
        for (int k = 0; k < n1; k++) {
            activities[k] = create.create(k, queue1, queue2);
        }
        for (int k = 0; k < n1; k++) {
            activities[k].start();
        }
        for (int k = 0; k < N; k++) {
            queue1.add(new Integer(k));
        }
        boolean[] result = new boolean[N];
        int failCount = 0;
        for (int k = 0; k < N; k++) {
            Integer res = queue2.poll(2, TimeUnit.SECONDS);
            if (res==null) {
                fail("timeout");
            }
            logger.info("got "+res);
            if (result[res]) {
                failCount++;
            }
            result[res] = true;
        }
        for (int k = 0; k < N; k++) {
            if (result[k]==false) {
                failCount++;
            }
        }
        Assert.assertEquals(0, failCount);
    }

    @Test
    public void testThread() throws InterruptedException {
        test1steps((int k, AsyncArrayBlockingQueue<Integer> inp, AsyncArrayBlockingQueue<Integer> out)-> new ThreadProcessor(inp, out));
    }

    @Test
    public void testAsync() throws InterruptedException {
        test1steps((int k, AsyncArrayBlockingQueue<Integer> inp, AsyncArrayBlockingQueue<Integer> out) -> new AsyncProcessor(k, inp, out));
    }

    @Test
    public void testMix() throws InterruptedException {
        test1steps((int k, AsyncArrayBlockingQueue<Integer> inp, AsyncArrayBlockingQueue<Integer> out) ->
//                k % 2 == 0 ? new ThreadProcessor(inp, out) : new AsyncProcessor(k, inp, out));
        k >= n1 ? new ThreadProcessor(inp, out) : new AsyncProcessor(k, inp, out));
    }

    @FunctionalInterface
    interface Creator {
        Activity create(int k, AsyncArrayBlockingQueue<Integer> queue1, AsyncArrayBlockingQueue<Integer> queue2);
    }

    class ThreadProcessor extends Thread implements ActivityThread {
        AsyncArrayBlockingQueue<Integer> inp;
        AsyncArrayBlockingQueue<Integer> out;

        public ThreadProcessor(AsyncArrayBlockingQueue<Integer> inp, AsyncArrayBlockingQueue<Integer> out) {
            this.inp = inp;
            this.out = out;
        }

        @Override
        public void run() {
            Throwable cause;
            for (;;) {
                try {
                    Integer in = inp.take();
                    Thread.sleep(getDelay());
                    out.put(in);
                } catch (InterruptedException e) {
                    cause = e;
                    break;
                } catch (CompletionException e) {
                    cause = e.getCause();
                    break;
                }
            }
            if (cause == null) {
                out.complete();
            } else {
                out.completeExceptionally(cause);
            }
        }
    }
    class AsyncProcessor extends Actor {
        InpFlow<Integer> inp = new InpFlow<>(this);
        OutChannel<Integer> out = new OutChannel<>(this);
        private final int n;

        public AsyncProcessor(int n, AsyncArrayBlockingQueue<Integer> inp, AsyncArrayBlockingQueue<Integer> out) {
            this.n = n;
            inp.subscribe(this.inp);
            out.feedFrom(this.out);
        }

        @Override
        public void runAction() {
            Throwable cause;
            try {
                Integer in = inp.remove();
                logger.info("AsyncProcessor "+n+": got "+in);
                Thread.sleep(getDelay());
                out.onNext(in);
                return;
            } catch (InterruptedException e) {
                cause = e;
            } catch (CompletionException e) {
                cause = e.getCause();
            }
            out.onError(cause);
            completeExceptionally(cause);
        }
    }
}


