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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class StreamOutputTest {

    @Parameterized.Parameters(name = "{index}: {0} => {1}*{2}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
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

    private int sourceNumber;
    private int sinkNumber;
    private int sinkCount;

    public StreamOutputTest(int sourceNumber, int sinkCount, int sinkNumber) {
        this.sourceNumber = sourceNumber;
        this.sinkNumber = sinkNumber;
        this.sinkCount = sinkCount;
    }

    @Test
    public void testUnicastSource() throws InterruptedException, ExecutionException {
        Logger parent = new Logger(true);
        String testName="1 sink "+sourceNumber+"."+sinkNumber;
        parent.println("=== test started:"+testName);
 //       Source<Long> from = new UnicastSource(parent, sourceNumber);
        Source<Long> from = new StreamSubscriptionSource(parent, sourceNumber);
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
            assertTrue(sinkCount>0);
        } catch (TimeoutException e) {
            assertTrue(sinkCount==0);
        }
        int expected = Math.min(sourceNumber, sinkCount*sinkNumber);
        int actual = 0;
        for (int k=0; k<sinkCount; k++) {
            LoggingSink sink = sinks.get(k);
            assertTrue(sink.name+" not completed", sink.completed);
            actual+= sink.received.get();
        }
        assertEquals(expected, actual);
        parent.println("=== test ended:"+testName+'\n');
    }
}
