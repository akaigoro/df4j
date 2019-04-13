/*
 * Copyright 2012 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.core.actor;

import org.df4j.core.asyncproc.AsyncResult;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class StreamOutputTest {

    public static Iterable<int[]> data() {
        return Arrays.asList(new int[][]{
                {0, 1, 1},
                {1, 2, 2},
                {4, 5, 3},
                {9, 10, 4},
                {1, 0, 1},
                {5, 4, 3},
                {0, 1, 2},
                {1, 1, 3},
                {2, 2, 4},
                {5, 5, 5},
                {2, 1, 2},
                {8, 2, 4},
                {10, 5, 2}
        });
    }

    public void testSource(int sourceNumber, int sinkCount, int sinkNumber,
                                          BiFunction<Integer, Logger, Source<Long>> createSource) throws InterruptedException, ExecutionException {
        Logger parent = new Logger(true);
        String testName="source count:"+sourceNumber+"; sinks:"+sinkCount+"; sink number:"+sinkNumber;
        parent.println("=== test started:"+testName);
        Source<Long> from = createSource.apply(sourceNumber, parent);
        ArrayList<LoggingSink> sinks = new ArrayList<>();
        for (int k=0; k<sinkCount; k++) {
            LoggingSink to = new LoggingSink(parent,Integer.MAX_VALUE,"sink"+k);
            sinks.add(to);
            from.subscribe(to);
        }
        from.start();
        AsyncResult result = from.asyncResult();
        try {
            result.get(200, TimeUnit.MILLISECONDS);
            if (sinkCount==0) {
                parent.println("no sinks, but result.get()succseeds-> no TimeoutException");
            }
        } catch (TimeoutException e) {
            if (sinkCount==0) {
                parent.println("no sinks, TimeoutException as expected");
            } else {
                for (int k=0; k<sinkCount; k++) {
                    LoggingSink sink = sinks.get(k);
                    assertTrue(sink.name+" not completed", sink.completed);
                }
                fail("result.get()->TimeoutException");
            }
        }
        int expected = Math.min(sourceNumber, sinkCount*sinkNumber);
        int actual = 0;
        for (int k=0; k<sinkCount; k++) {
            LoggingSink sink = sinks.get(k);
            actual+= sink.received.get();
        }
        assertEquals(expected, actual);
        parent.println("=== test ended:"+testName+'\n');
    }

    public void testSource(BiFunction<Integer, Logger, Source<Long>> createSource) throws InterruptedException, ExecutionException {
        for (int[] row: data()) {
            testSource(row[0], row[1], row[2], createSource);
        }
    }

    @Test
    public void test011() throws InterruptedException, ExecutionException {
        testSource(0,1,1,(sourceNumber, parent) -> new UnicastBufferedSource(parent, sourceNumber));
    }

    @Test
    public void test101() throws InterruptedException, ExecutionException {
        testSource(1,0,1,(sourceNumber, parent) -> new UnicastBufferedSource(parent, sourceNumber));
    }

    @Test
    public void testUnBufferedSource() throws InterruptedException, ExecutionException {
        testSource((sourceNumber, parent) -> new UnicastUnbufferedSource(parent, sourceNumber));
    }

    @Test
    public void testBufferedSource() throws InterruptedException, ExecutionException {
        testSource((sourceNumber, parent) -> new UnicastBufferedSource(parent, sourceNumber));
    }
}
