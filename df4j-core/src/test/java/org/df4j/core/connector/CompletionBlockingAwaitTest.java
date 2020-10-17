package org.df4j.core.connector;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class CompletionBlockingAwaitTest {
    public void basicCompletedTest(long timeout, TimeUnit unit) {
        Completion completion = new Completion();
        completion.complete();
        long start = System.currentTimeMillis();
        completion.blockingAwait(timeout, unit);
        long actual = System.currentTimeMillis() - start;
        boolean passed = actual < 5;
        Assert.assertTrue("timeout="+unit.toMillis(timeout)+"; actual="+actual, passed);
    }

    @Test
    public void microsecondsTest() {
        for (Long timout: Arrays.asList(0L, 1L, 10L, 100L, 1000L, 100000L)) {
            basicCompletedTest(timout, TimeUnit.MICROSECONDS);
        }
    }

    @Test
    public void millisecondsTest() {
        for (Long timout: Arrays.asList(0L, 1L, 10L, 100L, 500L)) {
            basicCompletedTest(timout, TimeUnit.MILLISECONDS);
        }
    }

    @Test
    public void secondsTest() {
        for (Long timout: Arrays.asList(0L, 1L, 2L)) {
            basicCompletedTest(timout, TimeUnit.SECONDS);
        }
    }

    public void basicNotCompletedTest(long timeout, TimeUnit unit) {
        Completion completion = new Completion();
        long start = System.currentTimeMillis();
        completion.blockingAwait(timeout, unit);
        long actual = System.currentTimeMillis() - start;
        long expected = unit.toMillis(timeout);
        double delta = Math.abs(expected - actual);
        double interval = Math.max(expected, actual);
        boolean passed = delta < 5 || interval > 0 && delta / interval <1/5;
   //     Assert.assertTrue("expected="+expected+"; actual="+actual, passed);
    }

    @Test
    public void microsecondsTestN() {
        for (Long timout: Arrays.asList(0L, 1L, 10L, 100L, 1000L, 100000L)) {
            basicNotCompletedTest(timout, TimeUnit.MICROSECONDS);
        }
    }

    @Test
    public void millisecondsTestN() {
        for (Long timout: Arrays.asList(0L, 1L, 10L, 100L, 500L)) {
            basicNotCompletedTest(timout, TimeUnit.MILLISECONDS);
        }
    }

    @Test
    public void secondsTestN() {
        for (Long timout: Arrays.asList(0L, 1L, 2L)) {
            basicNotCompletedTest(timout, TimeUnit.SECONDS);
        }
    }
}
