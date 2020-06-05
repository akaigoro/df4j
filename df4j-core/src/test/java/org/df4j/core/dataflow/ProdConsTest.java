package org.df4j.core.dataflow;

import org.df4j.core.activities.ConsumerActor;
import org.df4j.core.activities.ProducerActor;
import org.junit.Assert;
import org.junit.Test;

public class ProdConsTest {

    public void testProdCons(int cnt, int delay1, int delay2) throws InterruptedException {
        ProducerActor prod = new ProducerActor(cnt, delay1);
        ConsumerActor cons = new ConsumerActor(delay2);
        cons.inp.feedFrom(prod.out);
        prod.start();
        cons.start();
        boolean fin = cons.blockingAwait(Math.max(delay1, delay2)*cnt+100);
        Assert.assertTrue(cons.isCompleted());
    }

    @Test
    public void testComplete() throws InterruptedException {
        testProdCons(0,0, 0);
    }

    @Test
    public void testSlowProdCons() throws InterruptedException {
        testProdCons(5,100, 0);
    }

    @Test
    public void testProdSlowCons() throws InterruptedException {
        testProdCons(5,0, 50);
    }
}


