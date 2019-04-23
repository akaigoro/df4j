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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.function.BiFunction;

import org.df4j.core.asyncproc.AsyncProc.CurrentThreadExecutor;
import org.reactivestreams.Publisher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public abstract class StreamOutputTestBase {

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

    protected abstract Source<Long> createPublisher(long elements);

    public void testSource(int sourceNumber, int sinkCount, int sinkNumber) throws InterruptedException, ExecutionException {
        String testName="source count:"+sourceNumber+"; sinks:"+sinkCount+"; sink number:"+sinkNumber;
        Source<Long> from = createPublisher(sourceNumber);
        Logger log = from.log;
        log.println("=== test started:"+testName);
        ArrayList<LoggingSink> sinks = new ArrayList<>();
        for (int k=0; k<sinkCount; k++) {
            LoggingSink to = new LoggingSink(log,Integer.MAX_VALUE,"sink"+k);
            sinks.add(to);
            from.subscribe(to);
        }
        CurrentThreadExecutor executor = new CurrentThreadExecutor(); // to debug
  //      from.setExecutor(executor);
        from.start();
 //       executor.runAll();
        AsyncResult result = from.asyncResult();
        try {
            log.get(100, TimeUnit.MILLISECONDS);
            if (sinkCount==0) {
                log.println("no sinks, but result.get()succseeds-> no TimeoutException");
            }
        } catch (TimeoutException e) {
            if (sinkCount==0) {
                log.println("no sinks, TimeoutException as expected");
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
            int delta = sink.received.get();
      //      log.println(sink.toString()+'\n');
            actual+= delta;
        }
        assertEquals(expected, actual);
        log.println("=== test ended:"+testName+'\n');
    }

    public void testSource() throws InterruptedException, ExecutionException {
        for (int[] row: data()) {
            testSource(row[0], row[1], row[2]);
        }
    }
}

