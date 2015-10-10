package org.df4j.pipeline.core;

import java.nio.CharBuffer;
import java.nio.charset.CoderResult;
import org.df4j.pipeline.core.BufTransformer;

public class CharBufCopyTransformer extends BufTransformer<CharBuffer, CharBuffer> {
    
    public CharBufCopyTransformer(int buflen) {
        if (buflen<=0) {
            throw new IllegalArgumentException();
        }
        
        for (int k = 0; k < 2; k++) {
            CharBuffer buf = CharBuffer.allocate(buflen);
            myOutput.post(buf);
        }
    }

    protected CoderResult transformBuffers(CharBuffer inbuf, CharBuffer outbuf) {
        for (;;) {
            char ch = inbuf.get();
            outbuf.put(ch);
            if (!inbuf.hasRemaining()) {
                return CoderResult.UNDERFLOW;
            }
            if (!outbuf.hasRemaining()) {
                return CoderResult.OVERFLOW;
            }
        }
    }
    
    @Override
    protected CoderResult completeBuffer(CharBuffer outbuf) {
        if (outbuf!=null) {
            if (outbuf.position()>0) {
                // send outbuf
                outbuf.flip();
                sinkPort.post(outbuf);
            }            
        }
        sinkPort.close();
        // delete own buffers
        myOutput.close();
        while (myOutput.moveNext());
        return CoderResult.UNDERFLOW; // means success
    }
}