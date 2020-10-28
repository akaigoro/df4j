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

import org.df4j.core.actor.ActorGroup;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

/**
 * Sequential file writer.
 */
public class AsyncFileWriter extends AsyncFileChannel {

    private static final ExecutorService execService = ForkJoinPool.commonPool();

    public AsyncFileWriter(ActorGroup actorGroup, AsynchronousFileChannel channel, int capacity) {
        super(actorGroup, channel, capacity);
    }

    public AsyncFileWriter(AsynchronousFileChannel fileChannel, int capacity) {
        this(new ActorGroup(), fileChannel, capacity);
    }

    public AsyncFileWriter(ActorGroup actorGroup, Path path, int capacity) throws IOException {
        this(actorGroup,
                AsynchronousFileChannel.open(path, new HashSet<OpenOption>(Arrays.asList(StandardOpenOption.WRITE)), execService),
                capacity);
    }

    @Override
    protected void doIO(ByteBuffer buffer) {
        channel.write(buffer, filePosition, buffer, this);
    }
}
