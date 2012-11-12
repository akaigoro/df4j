/* Copyright 2011-2012 by Alexei Kaigorodov
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
import java.util.concurrent.atomic.AtomicInteger;

import com.github.rfqu.df4j.core.Port;
import com.github.rfqu.df4j.core.Request;

/**
 * Request for an I/O operation.
 * @param <R> actual type of the request, after subclassing.
 */
public class IORequest<R extends IORequest<R>> extends Request<R, Integer> {
    public static final AtomicInteger ids=new AtomicInteger(); // DEBUG

    public int rid=ids.addAndGet(1);
    protected ByteBuffer buffer;
    private boolean inRead;
    private boolean inTrans=false;

	public IORequest(ByteBuffer buffer) {
		this.buffer = buffer;
	}

    public void prepare(boolean read, Port<R> replyTo) {
        super.prepare(replyTo);
        this.inRead=read;
        if (read) {
            buffer.clear();
        } else {
            buffer.flip();
        }
        if (buffer.remaining()==0) {
            throw new IllegalArgumentException("no free space in the buffer");
        }
    }

    public void clear() {
//        checkInTrans();
        if (buffer!=null) {
            buffer.clear();
        }
    }

    /** for timer */
//    @Override
    public synchronized void run() {
        reply();
    }

    private void checkInTrans() {
        if (inTrans) {
            throw new IllegalStateException("in transfer state already");
        }
        inTrans=true;
    }

	public synchronized void completed(Integer result) {
//        System.err.println(" IORequest.completed "+state+" rid="+rid);
        checkInTrans();
        if (inRead) {
            //System.out.println("channel read completed id="+id);
            buffer.flip();
        } else {
        	//System.out.println("channel write completed id="+id);
            buffer.clear();
        }
        inTrans=false;
        reply(result);
	}

    public synchronized void failed(Throwable exc) {
        checkInTrans();
        this.exc=exc;
        inTrans=false;
        reply();
    }

    public void setBuffer(ByteBuffer buf) {
        this.buffer = buf;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

	public boolean isReadOp() {
		return inRead;
	}

	public boolean isInTrans() {
        return inTrans;
	}
}
