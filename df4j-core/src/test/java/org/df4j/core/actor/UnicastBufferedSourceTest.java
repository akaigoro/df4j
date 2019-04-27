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

import io.reactivex.Completable;
import org.junit.Test;

import java.util.concurrent.*;

public class UnicastBufferedSourceTest extends StreamOutputTestBase {
    Completable c;

    @Override
    public Source<Long> createPublisher(long elements) {
        Logger parent = new Logger(true);
        Source flowPublisher = new UnicastBufferedSource(parent, elements);
        return flowPublisher;
    }

    @Test
    public void bufferedSourceRegressionTest() throws InterruptedException, ExecutionException {
        testSource(1,2,2);
        testSource(5,4,3);
        testSource(9,10,4);
    }

    @Test
    public void testBufferedSource() throws InterruptedException, ExecutionException {
        for (int[] row: data()) {
            testSource(row[0], row[1], row[2]);
        }
    }
}

