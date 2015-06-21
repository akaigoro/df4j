package org.df4j.pipeline.core;

import java.nio.Buffer;
import java.nio.charset.CoderResult;
import java.util.concurrent.Executor;
import org.df4j.pipeline.df4j.core.StreamPort;

public abstract class BufTransformer<I extends Buffer, O extends Buffer>
    extends TransformerNode<I, O>
{
    /** here output messages return */
    protected StreamInput<O> myOutput=new StreamInput<O>();

    public BufTransformer() {
    }

    public BufTransformer(Executor executor) {
        super(executor);
    }
    
    @Override
    public StreamPort<O> getReturnPort() {
        return myOutput;
    }

	@Override
    protected void act() {
        I inbuf=input.get();
        O outbuf=myOutput.get();
        outbuf.clear();
        // processing loop:
        while (inbuf!=null) {
            CoderResult res=transformBuffers(inbuf, outbuf);

            if (res.isUnderflow()) {
                free(inbuf);  // free inbuf
                if (!input.moveNext()) {
                    if (outbuf.position()==0) {
                        myOutput.pushback();
                    } else {
                        // send outbuf
                        outbuf.flip();
                        sinkPort.post(outbuf);
                    }
                    return;
                }
                inbuf=input.get();
            }
            if (res.isOverflow()) {
                // send outbuf
                outbuf.flip();
                sinkPort.post(outbuf);
                if (!myOutput.moveNext()) {
                    input.pushback();
                    return;
                }
                outbuf=myOutput.get();
                outbuf.clear();
            }
        }
        // completing loop
        for (;;) {
            CoderResult res = completeBuffer(outbuf);
            if (res.isUnderflow()) {
            	break;
            }
            // need more space to flush data
            input.pushback();
            // send outbuf
            outbuf.flip();
            sinkPort.post(outbuf);
            if (!myOutput.moveNext()) {
                input.pushback();
                return;
            }
            outbuf=myOutput.get();
            outbuf.clear();
        }
        sinkPort.close();
        // delete own buffers
        myOutput.close();
        while (myOutput.moveNext());
    }

    @Override
    protected void act(I message) throws Exception {
        throw new UnsupportedOperationException();
    }

    /**
     * gets two usable buffers
     * @param inbuf
     * @param outbuf
     * @return bit mask with at least one bit set
     *    if UNDERFLOW is set, new inbuf required
     *    if OVERFLOW  is set, new outbuf required
     */
    protected abstract CoderResult transformBuffers(I inbuf, O outbuf);
    
    
    /**
     * input is closed; pass closing signal further
     * @param outbuf 
     * @param outmessage 
     * @return 
     */
    protected abstract CoderResult completeBuffer(O outbuf);

}