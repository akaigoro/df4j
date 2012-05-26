/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.ext;
import com.github.rfqu.df4j.core.*;
import com.github.rfqu.df4j.ext.PrivateExecutor;
import com.github.rfqu.df4j.ext.SwingSupport;

import static org.junit.Assert.assertEquals;
import java.util.concurrent.Executor;
import org.junit.Test;
import com.github.rfqu.df4j.util.DoubleValue;

public class ActorVariantsTest {
    private static final double delta = 1E-14;

    /**
     * computes sum and average of input values
     */
	class Aggregator extends Actor<DoubleValue> {
	    Aggregator(Executor executor) {
	        super(executor);
	    }

	    Aggregator() {
	    }

	    double _sum=0.0;
	    long counter=0;
	    
        // outputs
        Demand<Double> sum=new Demand<Double>();
        Demand<Double> avg=new Demand<Double>();

        @Override
        protected void act(DoubleValue message) throws Exception {
            counter++;
            _sum+=message.value;
        }
        
        @Override
        protected void complete() throws Exception {
            sum.send(_sum);
            avg.send(_sum/counter);
        }

    }
	
    public void runTest(Executor executor) throws InterruptedException {
        Aggregator node = new Aggregator(executor);
        PortFuture<Double> sum=new PortFuture<Double>();
        PortFuture<Double> avg=new PortFuture<Double>();
        {
            node.sum.connect(sum);
            node.avg.connect(avg);
        }
        double value=1.0;
        int cnt=12456;
        for (int k=0; k<cnt; k++) {
            value/=2;
            node.send(new DoubleValue(value));
        }
        node.close(); // causes node.complete()
        assertEquals(1.0, sum.get(), delta);
        assertEquals(1.0/cnt, avg.get(), delta);
    }
    
    // normal actor
    @Test
    public void t01() throws InterruptedException {
    	runTest(ThreadFactoryTL.newSingleThreadExecutor());
    }
    
    // eager actor
    @Test
    public void t02() throws InterruptedException {
    	runTest(null);
    }
    
    // fat actor
    @Test
    public void t03() throws InterruptedException {
        runTest(new PrivateExecutor());
    }

    // swing actor
    @Test
    public void t04() throws InterruptedException {
        runTest(SwingSupport.getSwingExecutor());
    }
}


