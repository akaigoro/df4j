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

import org.df4j.core.ExceptionHandler;
import org.df4j.core.Port;
import org.df4j.core.func.Promise;
import org.junit.Assert;
import org.junit.Test;

public class ProxyActorTest {
	
	static interface IAccum {
		void add(int delta);
		void sub(int delta);
		void throwError(Throwable err);
		void get(Port<Integer> port);
		public void getException(Port<Throwable> port);
	}

    static class Accum implements IAccum, ExceptionHandler {
		int sum=0;
		Throwable savedException;
		
        @Override
		public void add(int delta) {
        	System.out.println("add from "+Thread.currentThread().getName());
			sum+=delta;
		}
		
		@Override
		public void sub(int delta) {
        	System.out.println("sub from "+Thread.currentThread().getName());
			sum-=delta;
		}

		@Override
		public void throwError(Throwable err) {
			ProxyActor.throwUncheked(err);
		}
		
		@Override
		public void get(Port<Integer> port) {
        	System.out.println("get from "+Thread.currentThread().getName());
            port.post(sum);
		}
		
		@Override
		public void getException(Port<Throwable> port) {
            port.post(savedException);
		}

		@Override
		public void handleException(Throwable e) {
			savedException=e;
		}

    }

    @Test
    public void runTest() throws InterruptedException, ExecutionException {
    	System.out.println("runTest from "+Thread.currentThread().getName());
		IAccum acc=ProxyActor.makeProxy(IAccum.class, new Accum());
        acc.add(11);
        acc.sub(9);
        
        // take intermediate result
        Promise<Integer> res=new Promise<Integer>();
        acc.get(res);

        acc.add(8);
    	Assert.assertEquals(Integer.valueOf(2), res.get());
   }

    @Test
    public void errHandlereTest1() throws InterruptedException, ExecutionException {
    	System.out.println("closeTest from "+Thread.currentThread().getName());
		IAccum acc=ProxyActor.makeProxy(IAccum.class, new Accum());
		final Error error = new Error("intentional");
        Promise<Throwable> res=new Promise<Throwable>();
        acc.throwError(error);
        acc.getException(res);
    	Assert.assertEquals(error, res.get());
    }

    @Test
    public void errHandlereTest2() throws InterruptedException, ExecutionException {
    	System.out.println("closeTest from "+Thread.currentThread().getName());
		IAccum acc=ProxyActor.makeProxy(IAccum.class, new Accum());
		final Error error = new Error("intentional");
		Port<Integer> errSource=new Port<Integer>() {
			@Override
			public void post(Integer message) {
				throw error;
			}
		};
        Promise<Throwable> res=new Promise<Throwable>();
        acc.get(errSource);
        acc.getException(res);
    	Assert.assertEquals(error, res.get());
    }

    public static void main(String args[]) throws InterruptedException, ExecutionException {
        ProxyActorTest nt = new ProxyActorTest();
        nt.runTest();
    }

}

