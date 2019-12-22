package org.df4j.core.dataflow;

import org.df4j.core.activities.Consumer;
import org.df4j.core.activities.Producer;
import org.junit.Assert;
import org.junit.Test;

public class ProdConsTest {

    public void testProdCons(int cnt, int delay1, int delay2) throws InterruptedException {
        Consumer cons = new Consumer(delay2);
        Producer prod = new Producer(cnt, cons.inp, delay1);
        prod.awake();
        cons.awake();
        boolean fin = cons.blockingAwait(1000);
        Assert.assertTrue(fin);
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
        testProdCons(5,0, 100);
    }
}


