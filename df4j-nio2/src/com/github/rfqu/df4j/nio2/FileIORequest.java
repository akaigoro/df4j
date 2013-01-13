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
package com.github.rfqu.df4j.nio2;

import java.nio.ByteBuffer;

import com.github.rfqu.df4j.nio.IORequest;

/**
 * Request for a file I/O operation.
 */
public class FileIORequest<R extends FileIORequest<R>>
  extends IORequest<R>
{
    private long position;
    
    public FileIORequest(ByteBuffer buf) {
        super(buf);
    }

    public void prepareRead(long position){
        super.prepareRead();
        this.position = position;
    }

    public void prepareWrite(long position){
        super.prepareWrite();
        this.position = position;
    }

    public long getPosition() {
        return position;
    }

}
