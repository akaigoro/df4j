/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.core;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Test;

public class LinkedQueueTest {

    @Test
    public void test1() throws InterruptedException, ExecutionException {
        DoublyLinkedQueue<Message> q=new DoublyLinkedQueue<Message>();
    	Assert.assertEquals(0, q.size());
        Assert.assertEquals(null, q.poll());
        Message m1 = new Message();
        Message m2 = new Message();
        q.offer(m1);
        Assert.assertEquals(1, q.size());
        Assert.assertEquals(m1, q.peek());
        Assert.assertEquals(1, q.size());
        try {
            q.offer(m1);
            Assert.fail();
        } catch (IllegalArgumentException e) {
        }
        Assert.assertEquals(m1, q.poll());
        Assert.assertEquals(0, q.size());
        q.offer(m1);
        q.offer(m2);
        Assert.assertEquals(m1, q.peek());
        Assert.assertEquals(m1, q.poll());
        Assert.assertEquals(m2, q.poll());
        Assert.assertEquals(null, q.poll());
        Assert.assertEquals(0, q.size());
    }

    @Test
    public void test2() throws InterruptedException, ExecutionException {
        DoublyLinkedQueue<Message> q=new DoublyLinkedQueue<Message>();
        int count=q.size();
        Assert.assertEquals(0, count);
        Message[] mm=new Message[2];
        mm[0] = new Message();
        mm[1] = new Message();
        q.add(mm[0]);
        q.offer(mm[1]);
        Iterator<Message> it=q.iterator();
        while (it.hasNext()) {
            Message m=it.next();
            Assert.assertEquals(m, mm[count]);
            count++;
        }
        Assert.assertEquals(2, count);
    }
    
    @Test
    public void test21() throws InterruptedException, ExecutionException {
        LinkedList<Message> q=new LinkedList<Message>();
        int count=q.size();
        Assert.assertEquals(0, count);
        Message[] mm=new Message[2];
        mm[0] = new Message();
        mm[1] = new Message();
        q.add(mm[0]);
        q.offer(mm[1]);
//        for (Message m: q) {
        Iterator<Message> it=q.iterator();
        while (it.hasNext()) {
            Message m=it.next();
            Assert.assertEquals(m, mm[count]);
            count++;
        }
        Assert.assertEquals(2, count);
    }
    
    static class Message extends Link {
        int id;
    }

}
