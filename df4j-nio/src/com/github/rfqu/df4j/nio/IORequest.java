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
package com.github.rfqu.df4j.nio;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;

import com.github.rfqu.df4j.ext.Request;

/**
 * Request for an I/O operation.
 * @param <T> actual type of the request, after subclassing.
 */
public class IORequest<T extends IORequest<T>> extends Request<T, Integer> {
    protected ByteBuffer buffer;
    private boolean inRead;

	public IORequest(ByteBuffer buffer) {
		this.buffer = buffer;
	}

    public void prepareRead() {
        super.reset();
        this.inRead=true;
        buffer.clear();
        if (buffer.remaining()==0) {
            throw new IllegalArgumentException("no free space in the buffer");
        }
    }

    public void prepareWrite() {
        super.reset();
        this.inRead=false;
        buffer.flip();
        if (buffer.remaining()==0) {
            throw new IllegalArgumentException("no free space in the buffer");
        }
    }

    public void clear() {
        if (buffer!=null) {
            buffer.clear();
        }
    }
    
    @SuppressWarnings("unchecked")
    public void toIOCallback(IOCallback<T> handler) {
//      public void toCallback(IOCallback<IORequest<T>> handler) {
//        IORequest<T> r =  this;
		T r =  (T)this;
        if (exc == null) {
            if (value==-1) {
                handler.closed(r);
            } else {
                handler.completed(value, r);
            }
        } else {
            if (exc instanceof AsynchronousCloseException) {
                handler.closed(r);
// TODO                
//            } else if (exc instanceof InterruptedByTimeoutException) {
//                handler.timedOut(r);
            } else {
                handler.failed(exc, r);
            }
        }
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

    //======================== backend methods - called from socket handler 

	public synchronized void post(Integer result) {
//        System.err.println(" IORequest.completed "+state+" rid="+rid);
        if (inRead) {
            //System.out.println("channel read completed id="+id);
            buffer.flip();
        } else {
        	//System.out.println("channel write completed id="+id);
            buffer.clear();
        }
        super.post(result);
	}

    public synchronized void postFailure(Throwable exc) {
        super.postFailure(exc);
    }
}
