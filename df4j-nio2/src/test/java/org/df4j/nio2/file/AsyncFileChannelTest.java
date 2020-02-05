/*
 * Copyright 2011-2012 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.df4j.nio2.file;

import org.df4j.core.dataflow.Actor;
import org.df4j.core.dataflow.Dataflow;
import org.df4j.core.port.InpFlow;
import org.df4j.core.port.OutFlow;
import org.df4j.core.util.Utils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class AsyncFileChannelTest {

    static final long byteNunber = 2500;

    @Test
    public void testWriteAndRead() throws IOException, InterruptedException {
        int capacity = 5;
        Path path = Files.createTempFile("tetstfile", ".tmp");
        Dataflow dataflow = new Dataflow();
        Utils.CurrentThreadExecutor executor = new Utils.CurrentThreadExecutor();
  //      dataflow.setExecutor(executor); // for debug

        {
            DataProducer producer = new DataProducer(dataflow, capacity, byteNunber);
            AsyncFileWriter fileWriter = new AsyncFileWriter(dataflow, path, capacity);
            producer.filledBuffers.subscribe(fileWriter.input);
            fileWriter.output.subscribe(producer.emptyBuffers);
            producer.start();
            fileWriter.start();
            executor.executeAll(50);
            boolean finished = dataflow.blockingAwait(10, TimeUnit.MILLISECONDS);
            Assert.assertTrue(finished);
        }

        {
            AsyncFileReader fileReader = new AsyncFileReader(dataflow, path, capacity);
            for (int k = 0; k< capacity; k++) {
                fileReader.input.onNext(ByteBuffer.allocate(1024));
            }
            DataConsumer consumer = new DataConsumer(dataflow, capacity);
            fileReader.output.subscribe(consumer.filledBuffers);
            consumer.emptyBuffers.subscribe(fileReader.input);
            fileReader.start();
            consumer.start();
            executor.executeAll(50);
            boolean finished = dataflow.blockingAwait(100, TimeUnit.MILLISECONDS); // todo millis
            Assert.assertTrue(finished);
        }
    }

    static class DataProducer extends Actor {
        volatile long byteNumber;
        InpFlow<ByteBuffer> emptyBuffers;
        OutFlow<ByteBuffer> filledBuffers;
        volatile byte bt = 0;

        public DataProducer(Dataflow dataflow, int capacity, long byteNumber) {
            super(dataflow);
            this.byteNumber = byteNumber;
            emptyBuffers = new InpFlow<>(this, capacity);
            filledBuffers = new OutFlow<>(this, capacity);
            for (int k = 0; k< capacity; k++) {
                emptyBuffers.onNext(ByteBuffer.allocate(1024));
            }
        }

        byte nextByte() {
            bt++;
            return bt;
        }

        @Override
        protected void runAction() throws Throwable {
            ByteBuffer buf = emptyBuffers.removeAndRequest();
            while (buf.hasRemaining()) {
                buf.put(nextByte());
                byteNumber--;
                if (byteNumber == 0) {
                    break;
                }
            }
            buf.flip();
            filledBuffers.onNext(buf);
            if (byteNumber == 0) {
                filledBuffers.onComplete();
                onComplete();
            }
        }
    }

    static class DataConsumer extends Actor {
        InpFlow<ByteBuffer> filledBuffers;
        OutFlow<ByteBuffer> emptyBuffers;
        volatile byte bt = 0;

        public DataConsumer(Dataflow dataflow, int capacity) {
            super(dataflow);
            filledBuffers = new InpFlow<>(this, capacity);
            emptyBuffers = new OutFlow<>(this, capacity);
        }

        byte nextByte() {
            bt++;
            return bt;
        }

        @Override
        protected void runAction() throws Throwable {
            if (filledBuffers.isCompleted()) {
                onComplete();
                emptyBuffers.onComplete();
                return;
            }
            ByteBuffer buf = filledBuffers.removeAndRequest();
            while (buf.hasRemaining()) {
                byte nextByte = buf.get();
//                Assert.assertEquals(nextByte(), nextByte);
                byte nextByte1 = nextByte();
                if (nextByte1 != nextByte) {
                    Assert.fail("expected:"+nextByte1 + ": actual:"+nextByte);
                }
            }
            buf.flip();
            emptyBuffers.onNext(buf);
        }
    }
}
