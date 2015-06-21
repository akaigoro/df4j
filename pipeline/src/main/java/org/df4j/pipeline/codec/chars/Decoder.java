package org.df4j.pipeline.codec.chars;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import org.df4j.pipeline.core.BufTransformer;

/** 
 * Converts chars in ByteBuffers to butes in CharBuffers
 * @author kaigorodov
 *
 */
class Decoder extends BufTransformer <ByteBuffer, CharBuffer> {
	static final ByteBuffer emptyByteBuf=(ByteBuffer) ByteBuffer.wrap(new byte[]{}).flip();

    protected final Charset charset;
	protected final CharsetDecoder charSetDecoder;

    private boolean decoded;
    
    public Decoder(Charset charset) {
        this.charset=charset;
        charSetDecoder = charset.newDecoder();
        charSetDecoder.reset();
    }

	@Override
    protected CoderResult transformBuffers(ByteBuffer inBuf, CharBuffer outBuf) {
        CoderResult cr=charSetDecoder.decode(inBuf, outBuf, false);
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
    protected CoderResult completeBuffer(CharBuffer outBuf) {
        CoderResult cr;
        if (!decoded) {
            cr=charSetDecoder.decode(emptyByteBuf, outBuf, true);
            if (cr.isOverflow()) {
                return cr;
            }
            decoded=true;
        }
        cr=charSetDecoder.flush(outBuf);
        if (cr.isOverflow()) {
            return cr;
        }
        return CoderResult.UNDERFLOW;
   }

    public void injectBuffers(int bufCount, int buffLen) {
        for (int k = 0; k < bufCount; k++) {
            CharBuffer buf = CharBuffer.allocate(buffLen);
            myOutput.post(buf);
        }
    }
}