package org.df4j.pipeline.codec.chars;

import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import org.df4j.pipeline.core.BufTransformer;

/** 
 * Converts chars in ByteBuffers to butes in CharBuffers
 * @author kaigorodov
 *
 */
public class Encoder extends BufTransformer <CharBuffer, ByteBuffer> {
    static final CharBuffer emptyCharBuf=(CharBuffer) CharBuffer.wrap("");

    protected final Charset charset;
    protected final CharsetEncoder charSetEncoder;

    private boolean decoded;
    
    public Encoder(Charset charset) {
        this.charset=charset;
        charSetEncoder = charset.newEncoder();
        charSetEncoder.reset();
    }

    @Override
    protected CoderResult transformBuffers(CharBuffer inBuf, ByteBuffer outBuf) {
        CoderResult cr=charSetEncoder.encode(inBuf, outBuf, false);
        if (!cr.isUnderflow() && !cr.isOverflow()) {
            try {
                cr.throwException();
            } catch (CharacterCodingException e) {
                context.postFailure(e);
            }
        }
        return cr;
    }

    @Override
    protected CoderResult completeBuffer(ByteBuffer outBuf) {
        CoderResult cr;
        if (!decoded) {
            cr=charSetEncoder.encode(emptyCharBuf, outBuf, true);
            if (cr.isOverflow()) {
                return cr;
            }
            decoded=true;
        }
        cr=charSetEncoder.flush(outBuf);
        if (cr.isOverflow()) {
            return cr;
        }
        return CoderResult.UNDERFLOW;
   }

    public void injectBuffers(int bufCount, int buffLen) {
        for (int k = 0; k < bufCount; k++) {
            ByteBuffer buf = ByteBuffer.allocate(buffLen);
            myOutput.post(buf);
        }
    }
}
