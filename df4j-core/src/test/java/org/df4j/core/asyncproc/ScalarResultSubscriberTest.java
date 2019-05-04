package org.df4j.core.asyncproc;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ScalarResultSubscriberTest {

    @Test
    public void singlePublisherTest() throws InterruptedException, ExecutionException, TimeoutException {
        ScalarResult<Double> publisher = new ScalarResult<>();
        ScalarResult<Double> subscriber1 = new ScalarResult<>();
        ScalarResult<Double> subscriber2 = new ScalarResult<>();
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
