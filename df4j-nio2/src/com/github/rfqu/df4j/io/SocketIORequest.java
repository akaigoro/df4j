package com.github.rfqu.df4j.io;

import java.nio.ByteBuffer;

import com.github.rfqu.df4j.core.Link;

public class SocketIORequest extends Link {
    /** if not null, an exchange operation is in process on this channel */ 
    protected AsyncSocketChannel channel;
    /** the data buffer */
    protected ByteBuffer buffer;
    /** if true, the last started operation was reading */ 
    private boolean readOp;
    /** the result of the last operation, null if the operation is not completed or failed */
    protected Integer result;
    /** the failure of the last operation, null if the operation is not completed or succeeded */
    protected Throwable exc;
    
    /**
     * constructs request with newly created buffer
     * @param capacity the size of the buffer
     * @param direct if true, allocate direct buffer
     */
    public SocketIORequest(int capacity, boolean direct) {
        if (direct) {
            buffer=ByteBuffer.allocateDirect(capacity);
        } else {
            buffer=ByteBuffer.allocate(capacity);
        }
    }
    
    /**
     * constructs request with the given buffer
     * @param buffer data buffer
     */
    public SocketIORequest(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    /**
     * prepare this request for exchange
     * @param channel channel to exchange with
     * @param readOp if true then reading else writing 
     */
    void start(AsyncSocketChannel channel, boolean readOp) {
        if (this.channel!=null) {
            throw new IllegalStateException("SocketIORequest.read: in "+(readOp?"read":"write")+" already");
        }
        this.channel=channel;
        this.readOp=readOp;
        if (!readOp) {
            buffer.clear();
        }
        result=null;
        exc=null;
    }

    /**
     * successful request completion
     * @param result
     */
    protected void completed(Integer result) {
        if (channel==null) {
            // TODO
            throw new IllegalStateException("SocketIORequest "+(readOp?"read":"write")+" completed but not in trans");
        }
        this.result=result;
        buffer.flip();
        AsyncSocketChannel ch = channel;
        channel=null;
        ch.requestCompleted(this);
    }

    /**
     * request failed
     * @param exc failure exception
     */
    protected void failed(Throwable exc) {
        if (this.channel==null) {
            // TODO
            throw new IllegalStateException("SocketIORequest "+(readOp?"read":"write")+" failed but not in trans");
        }
        this.exc=exc;
        buffer.flip();
        AsyncSocketChannel ch = channel;
        channel=null;
        ch.requestCompleted(this);
    }

    /** getter for operation kind
     * @return true the last started operation was reading
     */
    public boolean isReadOp() {
        return readOp;
    }
    
    /** getter for the data buffer
     * @return buffer
     */
    public ByteBuffer getBuffer() {
        return buffer;
    }

    public Integer getResult() {
        return result;
    }

    public Throwable getExc() {
        return exc;
    }

}
