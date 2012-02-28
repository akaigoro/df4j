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

import java.io.PrintStream;
import java.util.concurrent.ExecutorService;

import org.junit.Test;

import com.github.rfqu.df4j.core.Link;
import com.github.rfqu.df4j.core.Port;
import com.github.rfqu.df4j.core.PortFuture;
import com.github.rfqu.df4j.core.Request;
import com.github.rfqu.df4j.core.Task;
import com.github.rfqu.df4j.core.ThreadFactoryTL;


/**
 * taken from Jactor
 * 
 */
public class ExecServiceTest {
	static final public class AddCount extends Request<AddCount> {
	    public long number;

		public AddCount(long number, Port<AddCount> replyTo) {
			super(replyTo);
			this.number = number;
		}
	}

	static final public class CounterActor extends EagerActor<Link> {
	    private long count = 0L;
	    {start();}

		@Override
		protected void act(Link request) throws Exception {
	        if (request instanceof AddCount) {
//				out.println("counterActor add");
	            AddCount addCount = (AddCount) request;
	            count += addCount.number;
	            addCount.reply(addCount);
	        } else if (request instanceof PortFuture) {
//				out.println("counterActor send");
	        	Long current = new Long(count);
	            count = 0;
	            PortFuture<Long> portFuture = (PortFuture<Long>)request;
				portFuture.send(current);
	        }
		}

		@Override
		protected void complete() throws Exception {
			// TODO Auto-generated method stub
			
		}
	}
	static final public class Driver extends EagerActor<AddCount> {
        long freeMemory;
        long usedMemory;
	    CounterActor counterActor;
	    PortFuture<Long> future;
	    long runs;

		public Driver(CounterActor counterActor, long runs, PortFuture<Long> future) {
			this.counterActor=counterActor;
			this.runs=runs;
			this.future=future;
		}

		public void start() {
            System.gc();
            freeMemory = Runtime.getRuntime().freeMemory();
			long nm=(long) Math.sqrt(runs);
			out.println("nm="+nm);
			for (int k=0; k<nm; k++) {
    			runs--;
				counterActor.send(new AddCount(k, this));
//				out.println("Driver counterActor.send 1");
			}
			super.start();
		}

		@Override
		protected void act(AddCount request) throws Exception {
            if (runs>0) {
    			runs--;
            	counterActor.send(request);
//				out.println("Driver counterActor.send 2 runs="+runs);
            } else if (runs==0 && future!=null) {
//				out.println("Driver counterActor.send future "+runs);
            	counterActor.send(future);
                System.gc();
                usedMemory = freeMemory-Runtime.getRuntime().freeMemory();
            	future=null;
            }
		}

		@Override
		protected void complete() throws Exception {
			// TODO Auto-generated method stub
			
		}
	}

    static final long runs = 10000000;
    final static int nThreads = Runtime.getRuntime().availableProcessors();
    final static PrintStream out=System.out;
    
    @Test
    public void testS() throws InterruptedException {
        runTest(new SimpleExecutorService());
    }

    @Test
    public void testSJUC() throws InterruptedException {
        runTest(ThreadFactoryTL.newSingleThreadExecutor());
    }

    @Test
    public void testJUC() throws InterruptedException {
        runTest(ThreadFactoryTL.newFixedThreadPool(nThreads));
    }

	private void runTest(ExecutorService executor) throws InterruptedException {
        out.println("Using " + executor.getClass().getCanonicalName());
		Task.setCurrentExecutor(executor);
        PortFuture<Long> future = new PortFuture<Long>();
        CounterActor counterActor=new CounterActor();
		Driver driver = new Driver(counterActor, runs, future);
        long start = System.currentTimeMillis();
        driver.start();
        Long count = future.get();
        long finish = System.currentTimeMillis();
        float elapsedTime = (finish - start) / 1000.0f;
        System.out.println("[java-shared] Number of runs: " + runs);
        System.out.println("[java-shared] Count: " + count);
        System.out.println("[java-shared] Test time in seconds: " + elapsedTime);
        System.out.println("[java-shared] Messages per microsecond: " + ((float)runs)/1000000 / elapsedTime);
        System.out.println("[java-shared] Used memory: " + driver.usedMemory);
    }
    
    public static void main(String args[]) throws Exception {
    	ExecServiceTest t=new ExecServiceTest();
    	t.testJUC();
    	t.testS();
    }
}
