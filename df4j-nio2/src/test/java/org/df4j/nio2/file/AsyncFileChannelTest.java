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

import org.df4j.core.actor.Actor;
import org.df4j.core.actor.ActorGroup;
import org.df4j.core.port.InpFlow;
import org.df4j.core.port.OutFlow;
import org.df4j.core.util.CurrentThreadExecutor;
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
        ActorGroup actorGroup = new ActorGroup();
        CurrentThreadExecutor executor = new CurrentThreadExecutor();
        actorGroup.setExecutor(executor); // for debug

        {
            DataProducer producer = new DataProducer(actorGroup, capacity, byteNunber);
            AsyncFileWriter fileWriter = new AsyncFileWriter(actorGroup, path, capacity);
            producer.filledBuffers.subscribe(fileWriter.input);
            fileWriter.output.subscribe(producer.emptyBuffers);
            producer.start();
            fileWriter.start();
            executor.executeAll(500);
            boolean finished = actorGroup.await(10, TimeUnit.MILLISECONDS);
            Assert.assertTrue(finished);
        }

        {
            AsyncFileReader fileReader = new AsyncFileReader(actorGroup, path, capacity);
            for (int k = 0; k< capacity; k++) {
                fileReader.input.onNext(ByteBuffer.allocate(1024));
            }
            DataConsumer consumer = new DataConsumer(actorGroup, capacity);
            fileReader.output.subscribe(consumer.filledBuffers);
            consumer.emptyBuffers.subscribe(fileReader.input);
            fileReader.start();
            consumer.start();
            executor.executeAll(50);
            boolean finished = actorGroup.await(100, TimeUnit.MILLISECONDS); // todo millis
            Assert.assertTrue(finished);
        }
    }

    static class DataProducer extends Actor {
        volatile long byteNumber;
        InpFlow<ByteBuffer> emptyBuffers;
        OutFlow<ByteBuffer> filledBuffers;
        volatile byte bt = 0;

        public DataProducer(ActorGroup actorGroup, int capacity, long byteNumber) {
            super(actorGroup);
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
            ByteBuffer buf = emptyBuffers.remove();
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
                complete();
            }
        }
    }

    static class DataConsumer extends Actor {
        InpFlow<ByteBuffer> filledBuffers;
        OutFlow<ByteBuffer> emptyBuffers;
        volatile byte bt = 0;

        public DataConsumer(ActorGroup actorGroup, int capacity) {
            super(actorGroup);
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
                complete();
                emptyBuffers.onComplete();
                return;
            }
            ByteBuffer buf = filledBuffers.remove();
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
