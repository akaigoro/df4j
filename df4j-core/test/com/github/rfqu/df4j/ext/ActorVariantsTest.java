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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Test;

import com.github.rfqu.df4j.core.Actor;
import com.github.rfqu.df4j.core.CallbackFuture;
import com.github.rfqu.df4j.core.DFContext;
import com.github.rfqu.df4j.testutil.DoubleValue;

/** An actor run by different executors
 */
public class ActorVariantsTest {
    private static final double delta = 1E-14;

    @After
    public void cleanCurrentThread() {
    	DFContext.removeCurrentContext();
    }
    
    /**
     * computes sum and average of input values
     * execution starts only after both demand pins are listened
     */
    static class Aggregator extends Actor<DoubleValue> {

        // outputs
        Demand<Double> sum=new Demand<Double>();
        Demand<Double> avg=new Demand<Double>();

        double _sum=0.0;
        long counter=0;

        public Aggregator(Executor executor) {
            super(executor);
        }

        public Aggregator() {
        }

        @Override
        protected void act(DoubleValue message) throws Exception {
            counter++;
            _sum+=message.value;
        }
        
        @Override
        protected void complete() throws Exception {
            sum.post(_sum);
            avg.post(_sum/counter);
        }
     
    }
   
    public void testA(Aggregator node) throws InterruptedException, ExecutionException, TimeoutException {
        CallbackFuture<Double> sumcf=new CallbackFuture<Double>();
        CallbackFuture<Double> avgcf=new CallbackFuture<Double>(node.avg);
        node.post(new DoubleValue(1.0));
        node.post(new DoubleValue(2.0));
        node.close();
        Double sumValue;
        try {
            // sumcf not ready as not connected to data source
            // and the node is not ready for execution  
            sumValue= sumcf.get(100); 
            fail("no TimeoutException");
        } catch (TimeoutException e) {
        }
        // check that the node did not start execution
        assertEquals(0, node.counter); 
        // trigger execution
        sumValue=sumcf.listenTo(node.sum).get(500000);
        assertEquals(3.0, sumValue, delta);
        assertEquals(1.5, avgcf.get(), delta);
    }

    public void testB(Aggregator node) throws InterruptedException, ExecutionException, TimeoutException {
        CallbackFuture<Double> sumcf=new CallbackFuture<Double>();
        CallbackFuture<Double> avgcf=new CallbackFuture<Double>(node.avg);
        double value=1.0;
        int cnt=12345;
        for (int k=0; k<cnt; k++) {
            value/=2;
            node.post(new DoubleValue(value));
        }
        node.close();
        Double sumValue;
        try {
            // sumcf not ready as not connected to data source
            // and the node is not ready for execution  
            sumValue= sumcf.get(100); 
            fail("no TimeoutException");
        } catch (TimeoutException e) {
        }
        // check that the node did not start execution
        assertEquals(0, node.counter); 
        // trigger execution
        sumValue=sumcf.listenTo(node.sum).get(500000);
        assertEquals(1.0, sumValue, delta);
        assertEquals(1.0/cnt, avgcf.get(), delta);
    }

    /** eager actor - ImmediateExecutor
     */
    @Test
    public void t00() throws InterruptedException, ExecutionException, TimeoutException {
        testB(new Aggregator(new ImmediateExecutor()));
    }
    
    /** normal actor - default executor
     */
    @Test
    public void t01() throws InterruptedException, ExecutionException, TimeoutException {
        testB(new Aggregator());
    }
    
    /** eager actor
     */
    @Test
    public void t02() throws InterruptedException, ExecutionException, TimeoutException {
        testA(new Aggregator(null));
    }
    
    /** fat actor
     */
    @Test
    public void t03() throws InterruptedException, ExecutionException, TimeoutException {
        testB(new Aggregator(new PrivateExecutor()));
    }

    /** swing actor
     */
    @Test
    public void t04() throws InterruptedException, ExecutionException, TimeoutException {
        testB(new Aggregator(SwingSupport.getSwingExecutor()));
    }

    public static void main(String args[]) throws TimeoutException, InterruptedException, ExecutionException {
        ActorVariantsTest qt = new ActorVariantsTest();
        qt.t02();
    }
}


