package org.df4j.core.asyncproc;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CompletablePromiseSubscribeTest {

    @Test
    public void singlePublisherTest() throws InterruptedException, ExecutionException, TimeoutException {
        CompletablePromise<Double> publisher = new CompletablePromise<>();
        CompletablePromise<Double> subscriber1 = new CompletablePromise<>();
        CompletablePromise<Double> subscriber2 = new CompletablePromise<>();
        publisher.subscribe(subscriber1);
        publisher.subscribe(subscriber2);
        double v = 4.0;
        publisher.onComplete(v);
        double val1 = subscriber1.get(1, TimeUnit.SECONDS).doubleValue();
        Assert.assertEquals(v, val1, 0.0001);
        double val2 = subscriber2.get(1, TimeUnit.SECONDS).doubleValue();
        Assert.assertEquals(v, val2, 0.0001);
    }
}
