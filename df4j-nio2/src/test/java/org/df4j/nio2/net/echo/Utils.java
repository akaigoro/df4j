package org.df4j.nio2.net.echo;

import org.df4j.nio2.net.ServerConnection;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class Utils {
    static final Charset charset = Charset.forName("UTF-16");

    public static ByteBuffer toByteBuf(String message) {
        return ByteBuffer.wrap(message.getBytes(charset));
    }

    public static String fromByteBuf(ByteBuffer b) {
        return new String(b.array(), charset);
    }


    public static void injectBuffers(int count, int bufLen, ServerConnection.Reader reader) {
        for (int k=0; k<count; k++) {
            ByteBuffer buf=ByteBuffer.allocate(bufLen);
            reader.input.onNext(buf);
        }
    }

}
