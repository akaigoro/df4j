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

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class ReactiveStreamUnicastTest extends ReactiveStreamExampleBase {

    public void testSourceToSink(int sourceNumber, int sinkNumber) throws Exception {
        Source<Long> from = new UnicastSource(this, sourceNumber);
        Sink to1 = new Sink(this, sinkNumber, "sink1");
        from.subscribe(to1);
        Sink to2 = new Sink(this, sinkNumber, "sink2");
        from.subscribe(to2);
        from.start();
        asyncResult().get(1, TimeUnit.SECONDS);
        // publisher always sends all tokens, even if all subscribers unsubscribed.
        int expected = Math.min(sourceNumber, 2*sinkNumber);
        int actual = to1.received + to2.received;
        assertEquals(expected, actual);
    }
}
