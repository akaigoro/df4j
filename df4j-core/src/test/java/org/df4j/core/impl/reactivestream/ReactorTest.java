/*
 * Copyright 2012 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.core.impl.reactivestream;

import org.junit.Test;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ReactorTest {

    public void testSourceToSink(int sourceNumber, int sinkNumber) throws InterruptedException {
        int totalNumber = Math.min(sourceNumber, sinkNumber);
        Source from = new Source(sourceNumber);
        Sink to = new Sink(sinkNumber);
        from.pub.subscribe(to.sub);
        assertTrue(to.fin.await(2, TimeUnit.SECONDS));
        assertEquals(totalNumber, from.sent);
        assertEquals(totalNumber, to.received);
    }

    @Test
    public void testSourceFirst() throws InterruptedException {
        testSourceToSink(0, 1);
 //       testSourceToSink(2, 1);
    }

    @Test
    public void testSinkFirst() throws InterruptedException {
        testSourceToSink(1, 0);
        testSourceToSink(10, 11);
    }

    @Test
    public void testSameTime() throws InterruptedException {
        testSourceToSink(0, 0);
        testSourceToSink(5, 5);
        testSourceToSink(50, 50);
    }

    static PrintStream out = System.out;
    static void println(String s) {
        out.println(s);
        out.flush();
    }

}
