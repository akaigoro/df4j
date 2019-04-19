package org.df4j.core.util.linked;

import org.junit.Assert;
import org.junit.Test;

public class LinkedIteratorTest {

    @Test
    public void toStringTest() {
        MyQueue queue = new MyQueue();
        String s1 = queue.toString();
        Assert.assertEquals("[]", s1);
        queue.offer(new MyLink(22));
        String s2 = queue.toString();
        Assert.assertEquals("[22]", s2);
        queue.offer(new MyLink(33));
        String s3 = queue.toString();
        Assert.assertEquals("[22, 33]", s3);
    }

    static class MyLink extends Link<MyLink> {
        final int value;

        MyLink(int value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return Integer.toString(value);
        }
    }
    static class MyQueue extends LinkedQueue<MyLink> {

    }
}
