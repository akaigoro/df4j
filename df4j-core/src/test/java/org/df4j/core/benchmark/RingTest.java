package org.df4j.core.benchmark;

import org.df4j.core.actor.AbstractProcessor;
import org.junit.Test;

public class RingTest {
    static class Transmitter extends AbstractProcessor<Long,Long> {
        {start();}
        @Override
        protected Long whenNext(Long item) throws Throwable {
            return item;
        }
    }
    static class EntryPoint extends AbstractProcessor<Long,Long> {
        int rounds=0;

        {start();}

        @Override
        protected Long whenNext(Long item) throws Throwable {
            rounds++;
            if (rounds%100==0) {
                complete();
            }
            return item;
        }
    }

    public void ring() throws InterruptedException {
        EntryPoint entryPoint = new EntryPoint();
        AbstractProcessor<Long,Long> pred=entryPoint;
        for (int k=1; k<1000; k++) {
            Transmitter next = new Transmitter();
            pred.subscribe(next);
            pred=next;
        }
        pred.subscribe(entryPoint);
        long startTime = System.currentTimeMillis();
        entryPoint.onNext(1l);
        entryPoint.await(12000);
        long elaps = System.currentTimeMillis() - startTime;
        System.out.println(""+100000+" passed in "+elaps+" ms");
    }

    @Test
    public void ringTest() throws InterruptedException {
        for (int k=0; k<4; k++){
            ring();
        }
     }
}
