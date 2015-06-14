package org.df4j.core;

import static org.junit.Assert.*;
import org.junit.Test;

public class SharedTokenTest {
    static class ShToken extends SharedToken<ShToken> {
        int id=44;
    }

    @Test
    public void test() {
        SharedToken<ShToken> token=new ShToken();
        Port<ShToken> consumer=(reply) -> {
            System.out.println("id="+reply.id);
            assertEquals(44, reply.id);
            reply.ret();
        };
        token.post(consumer);
        token.post(consumer);
    }

}
