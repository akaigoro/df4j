/*
 * Copyright 2012 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.nio2;

import java.util.concurrent.Executor;

/**
 * Handles the results of an IO operation.
 * Can be seen as a simplified actor, with space for only 1 incoming meassage.
 * @param <M> the type of accepted messages.
 */
public abstract class FileIOHandler extends IOHandler<FileIORequest, AsyncFileChannel> {

    public FileIOHandler(Executor executor) {
        super(executor);
    }

    public FileIOHandler() {
    }

}
