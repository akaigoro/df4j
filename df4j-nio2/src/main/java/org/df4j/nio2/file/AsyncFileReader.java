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

import org.df4j.core.dataflow.Dataflow;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;

/**
 * Sequential file reader.
 */
public class AsyncFileReader extends AsyncFileChannel {

    public AsyncFileReader(Dataflow dataflow, AsynchronousFileChannel channel, int capacity) {
        super(dataflow, channel, capacity);
    }

    public AsyncFileReader(AsynchronousFileChannel fileChannel, int capacity) {
        this(new Dataflow(), fileChannel, capacity);
    }

    @Override
    protected void doIO(ByteBuffer buffer) {
        channel.read(buffer, filePosition, buffer, this);
    }
}
