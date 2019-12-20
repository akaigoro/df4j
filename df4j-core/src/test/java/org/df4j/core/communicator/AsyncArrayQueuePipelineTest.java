package org.df4j.core.communicator;

import org.df4j.core.actor.Activity;
import org.df4j.core.actor.ActivityThread;
import org.df4j.core.actor.Actor;
import org.df4j.core.port.InpMessage;
import org.df4j.core.port.OutChannel;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;

public class AsyncArrayQueuePipelineTest {
    static final int N=20;
    static final int n1=1, n2=3;

    Random rand = new Random();
    private synchronized long getDelay() {
        return rand.nextInt(17);
    }

    public void test2steps(Creator create) throws InterruptedException {
        Activity[] activities = new Activity[n1+n2];
        AsyncArrayQueue<Integer> queue1 = new AsyncArrayQueue<Integer>(N);
        AsyncArrayQueue<Integer> queue2 = new AsyncArrayQueue<Integer>(N/4);
        AsyncArrayQueue<Integer> queue3 = new AsyncArrayQueue<Integer>(N);
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
            System.out.println("got "+res);
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
        AsyncArrayQueue<Integer> queue1 = new AsyncArrayQueue<Integer>(N);
        AsyncArrayQueue<Integer> queue2 = new AsyncArrayQueue<Integer>(N/4);
        for (int k = 0; k < n1; k++) {
            activities[k] = create.create(k, queue1, queue2);
        }
        for (int k = 0; k < n1; k++) {
            activities[k].start();
        }
        for (int k = 0; k < N; k++) {
            queue1.put(new Integer(k));
        }
        boolean[] result = new boolean[N];
        int failCount = 0;
        for (int k = 0; k < N; k++) {
            Integer res = queue2.poll(2, TimeUnit.SECONDS);
            if (res==null) {
                fail("timeout");
            }
            System.out.println("got "+res);
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
        test1steps((int k, AsyncArrayQueue<Integer> inp, AsyncArrayQueue<Integer> out)-> new ThreadProcessor(inp, out));
    }

    @Test
    public void testAsync() throws InterruptedException {
        test1steps((int k, AsyncArrayQueue<Integer> inp, AsyncArrayQueue<Integer> out) -> new AsyncProcessor(k, inp, out));
    }

    @Test
    public void testMix() throws InterruptedException {
        test1steps((int k, AsyncArrayQueue<Integer> inp, AsyncArrayQueue<Integer> out) ->
//                k % 2 == 0 ? new ThreadProcessor(inp, out) : new AsyncProcessor(k, inp, out));
        k >= n1 ? new ThreadProcessor(inp, out) : new AsyncProcessor(k, inp, out));
    }

    @FunctionalInterface
    interface Creator {
        Activity create(int k, AsyncArrayQueue<Integer> queue1, AsyncArrayQueue<Integer> queue2);
    }

    class ThreadProcessor extends Thread implements ActivityThread {
        AsyncArrayQueue<Integer> inp;
        AsyncArrayQueue<Integer> out;

        public ThreadProcessor(AsyncArrayQueue<Integer> inp, AsyncArrayQueue<Integer> out) {
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
                out.onComplete();
            } else {
                out.onError(cause);
            }
        }
    }
    class AsyncProcessor extends Actor {
        InpMessage<Integer> inp = new InpMessage<>(this);
        OutChannel<Integer> out = new OutChannel<>(this);
        private final int n;

        public AsyncProcessor(int n, AsyncArrayQueue<Integer> inp, AsyncArrayQueue<Integer> out) {
            this.n = n;
            ((Flow.Publisher<Integer>) inp).subscribe(this.inp);
            out.subscribe(this.out);
        }

        @Override
        public void runAction() {
            Throwable cause;
            try {
                Integer in = inp.remove();
                System.out.println("AsyncProcessor "+n+": got "+in);
                Thread.sleep(getDelay());
                out.onNext(in);
                return;
            } catch (InterruptedException e) {
                cause = e;
            } catch (CompletionException e) {
                cause = e.getCause();
            }
            out.onError(cause);
            stop(cause);
        }
    }
}


