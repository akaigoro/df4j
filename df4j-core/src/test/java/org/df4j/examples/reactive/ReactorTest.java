/*
 * Copyright 2012 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.examples.reactive;

import org.df4j.core.Actor1;
import org.df4j.core.Port;
import org.df4j.test.util.MessageSink;
import org.junit.Test;

import java.io.IOException;
import java.io.PrintStream;

/**
 * see http://benchmarksgame.alioth.debian.org/u64q/threadring-description.html#threadring
 * @author rfq
 *
 */
public class ReactorTest {

    @Test
    public void runTest() throws InterruptedException {
        Source from = new Source(7);
        Sink to = new Sink();
        from.pub.subscribe(to.sub);
    }

    static class Source extends Reactor {
        SingleStreamPublisher<Integer> pub = new SingleStreamPublisher<>();
        int val;

        public Source(int val) {
            this.val = val;
        }

        @Override
        protected void act() throws Exception {
            out.println("pub.post("+val+")");
            pub.post(val);
            val--;
            if (val == 0) {
                pub.close();
                out.println("pub.close()");
            }
        }
    }

    static class Sink extends Reactor {
        StreamSubscriber<Integer> sub = new StreamSubscriber<>();

        @Override
        protected void act() throws Exception {
            long val = sub.get();
            out.println("sub.get()="+val);
        }
    }

    static PrintStream out = System.out;
}
