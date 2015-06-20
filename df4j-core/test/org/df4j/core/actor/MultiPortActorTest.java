/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.core.actor;

import java.util.concurrent.ExecutionException;

import org.df4j.core.Port;
import org.df4j.core.actor.MultiPortActor;
import org.df4j.core.func.Promise;
import org.junit.Assert;
import org.junit.Test;

public class MultiPortActorTest {

    static class Accum extends MultiPortActor {
        int sum=0;
        Promise<Integer> res=new Promise<Integer>();
        
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
        
        final PortHandler<Port<Integer>> get=new PortHandler<Port<Integer>>() {
            @Override
            protected void act(Port<Integer> m) {
                m.post(sum);
            }
        };

		@Override
		protected void complete() {
            res.post(sum);
		}
        
    }

    @Test
    public void runTest() throws InterruptedException, ExecutionException {
        Accum acc=new Accum();
        acc.add.post(11);
        acc.sub.post(9);
        
        // take intermediate result
        Promise<Integer> res=new Promise<Integer>();
        acc.get.post(res);

        acc.add.post(8);
        acc.close();
    	Assert.assertEquals(new Integer(2), res.get());

        // take final result
        Assert.assertEquals(new Integer(10), acc.res.get());
   }

    @Test
    public void closeTest() throws InterruptedException, ExecutionException {
        Accum acc=new Accum();
        acc.add.post(11);
        acc.sub.post(9);
        acc.close();
        try {
			acc.sub.post(99);
		} catch (IllegalStateException ok) {
		}
    	Assert.assertEquals(new Integer(2), acc.res.get());
    }

    public static void main(String args[]) throws InterruptedException, ExecutionException {
        MultiPortActorTest nt = new MultiPortActorTest();
        nt.runTest();
    }

}
