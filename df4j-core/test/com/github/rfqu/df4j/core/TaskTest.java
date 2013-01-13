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

import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Test;

public class TaskTest {

    static class MyTask extends Task {
        CallbackFuture<Integer> res=new CallbackFuture<Integer>();

        @Override
        public void run() {
            res.post(11);
        }
    }

    @Test
    public void runTest() throws InterruptedException, ExecutionException {
        MyTask t=new MyTask();
        t.fire();
    	Assert.assertEquals(new Integer(11), t.res.get());
    }

    public static void main(String args[]) throws InterruptedException, ExecutionException {
        TaskTest nt = new TaskTest();
        nt.runTest();
    }

}
