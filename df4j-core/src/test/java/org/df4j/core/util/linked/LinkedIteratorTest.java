package org.df4j.core.util.linked;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class LinkedIteratorTest {

    @Test
    public void toStringTest() {
        MyQueue queue = new MyQueue();
        Assert.assertEquals("[]", queue.toString());
        Assert.assertEquals(0, queue.size());
        MyItem item22 = new MyItem(22);
        queue.offer(item22);
        Assert.assertEquals(1, queue.size());
        Assert.assertEquals("[22]", queue.toString());
        MyItem item33 = new MyItem(33);
        queue.offer(item33);
        Assert.assertEquals(2, queue.size());
        Assert.assertEquals("[22, 33]", queue.toString());
        queue.remove(item22);
        Assert.assertEquals(1, queue.size());
        Assert.assertEquals("[33]", queue.toString());
        queue.remove(item33);
        Assert.assertEquals("[]", queue.toString());
        Assert.assertEquals(0, queue.size());
    }

    static class MyItem extends LinkImpl<MyItem> {
        final int value;

        MyItem(int value) {
            this.value = value;
        }

        @Override
        public MyItem getItem() {
            return this;
        }

        @Override
        public String toString() {
            return Integer.toString(value);
        }
    }

    static class MyQueue extends LinkedQueue<MyItem> {
    }
}
