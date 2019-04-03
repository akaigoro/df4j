/*
 * Copyright 2012 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.core.reactivestream;

import org.df4j.core.scalar.AsyncProc;
import org.df4j.core.scalar.AllOf;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.PrintStream;

import static org.junit.Assert.assertEquals;

public abstract class ReactiveStreamExampleBase extends AllOf {
    @BeforeClass
    public static void init() {
        AsyncProc.setThreadLocalExecutor(AsyncProc.currentThreadExec);
        printOn();
    }

    public abstract void testSourceToSink(int sourceNumber, int sinkNumber) throws Exception;

    @Test
    public void testSourceLess() throws Exception {
        testSourceToSink(0, 1);
        testSourceToSink(1, 2);
        testSourceToSink(4, 5);
        testSourceToSink(9, 10);
    }

    @Test
    public void testSinkLess() throws Exception {
        testSourceToSink(1, 0);
        testSourceToSink(2, 1);
        testSourceToSink(5, 4);
        testSourceToSink(8, 7);
        testSourceToSink(10, 9);
    }

    @Test
    public void testSame() throws Exception {
        testSourceToSink(0, 0);
        testSourceToSink(0, 1);
        testSourceToSink(1, 0);
        testSourceToSink(1, 1);
        testSourceToSink(1, 2);
        testSourceToSink(2, 1);
        testSourceToSink(2, 2);
        testSourceToSink(5, 5);
    }

    static PrintStream out = System.out;
    static boolean printOn = false;

    static void printOn() {
        printOn = true;
    }

    static void println(String s) {
        if (!printOn) {
            return;
        }
        out.println(s);
        out.flush();
    }

}
