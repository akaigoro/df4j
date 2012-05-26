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

import org.junit.Assert;
import org.junit.Test;

public class MultiPortActorTest {

    static class Accum extends MultiPortActor {
        int sum;
        
        final PortHandler<Integer> add=new PortHandler<Integer>() {
            @Override
            protected void act(Integer m) {
               sum+=m;
            }
        };
        
        final PortHandler<Integer> sub=new PortHandler<Integer>() {
            @Override
            protected void act(Integer m) {
               sum-=m;
            }
        };
        
        final PortHandler<PortFuture<Integer>> get=new PortHandler<PortFuture<Integer>>() {
            @Override
            protected void act(PortFuture<Integer> m) {
                m.send(sum);
            }
        };
        
    }

    @Test
    public void runTest() throws InterruptedException {
        Accum acc=new Accum();
        acc.add.send(11);
        acc.sub.send(9);
        PortFuture<Integer> res=new PortFuture<Integer>();
        acc.get.send(res);
    	Assert.assertEquals(new Integer(2), res.get());
    }

    public static void main(String args[]) throws InterruptedException {
        MultiPortActorTest nt = new MultiPortActorTest();
        nt.runTest();
    }

}
