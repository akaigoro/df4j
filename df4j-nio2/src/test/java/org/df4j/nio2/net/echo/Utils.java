package org.df4j.nio2.net.echo;

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
}
