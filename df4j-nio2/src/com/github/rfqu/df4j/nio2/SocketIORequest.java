/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.nio2;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

import com.github.rfqu.df4j.core.Link;
import com.github.rfqu.df4j.core.Port;

public class SocketIORequest extends Link 
implements CompletionHandler<Integer, AsyncSocketChannel> {
    protected ByteBuffer buffer;
    protected AsyncSocketChannel channel;
    protected Port<SocketIORequest> replyTo;
    protected volatile boolean inTrans=false;
    protected boolean readOp;
    protected Integer result;
    protected Throwable exc;
    
    public SocketIORequest(int capacity, boolean direct) {
        if (direct) {
            buffer=ByteBuffer.allocateDirect(capacity);
        } else {
            buffer=ByteBuffer.allocate(capacity);
        }
    }
    
    public SocketIORequest(ByteBuffer buf) {
        this.buffer = buf;
    }
    
    public ByteBuffer getBuffer() {
        return buffer;
    }

    public AsyncSocketChannel getChannel() {
        return channel;
    }

    public boolean isInTrans() {
        return inTrans;
    }

    public boolean isReadOp() {
        return readOp;
    }

    public Integer getResult() {
        return result;
    }

    public Throwable getExc() {
        return exc;
    }

    public void start(AsyncSocketChannel channel, boolean read, Port<SocketIORequest> replyTo) {
        if (inTrans) {
            throw new IllegalStateException("SocketIORequest.read: in "+(readOp?"read":"write")+" already");
        }
        inTrans=true;
        this.channel=channel;
        readOp=read;
        this.replyTo=replyTo;
        if (read) {
            buffer.clear();
        } else {
            buffer.flip();
        }
        result=null;
        exc=null;
    }

    @Override
    public void completed(Integer result, AsyncSocketChannel channel) {
        inTrans=false;
        this.result=result;
        if (readOp) {
            //System.out.println("channel read completed id="+id);
            buffer.flip();
        } else {
            //System.out.println("channel write completed id="+id);
            buffer.clear();
        }
    }

    @Override
    public void failed(Throwable exc, AsyncSocketChannel channel) {
        inTrans=false;
        this.exc=exc;
    }

}
 