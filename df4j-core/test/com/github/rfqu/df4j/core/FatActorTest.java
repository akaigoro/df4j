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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FatActorTest {
    private static final double delta = 1E-14;

    static class Value extends Link {
        double value;

        public Value(double value) {
            this.value = value;
        }
        
    }
    
    /**
     * computes sum and average of input values
     */
	class Aggregator extends FatActor<Value> {
	    double _sum=0.0;
	    long counter=0;
        // outputs
        Demand<Double> sum=new Demand<Double>();
        Demand<Double> avg=new Demand<Double>();
        {super.start();}

        @Override
        protected void act(Value message) throws Exception {
            counter++;
            _sum+=message.value;
        }
        
        @Override
        protected void complete() throws Exception {
            sum.send(_sum);
            avg.send(_sum/counter);
        }

    }
    @Test
    public void t01() throws InterruptedException {
        Aggregator node = new Aggregator();
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
            node.send(new Value(value));
//            Thread.sleep(100500);
        }
        node.close();
        assertEquals(1.0, sum.get(), delta);
        assertEquals(1.0/cnt, avg.get(), delta);
    }
}


