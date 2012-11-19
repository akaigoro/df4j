/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.tutorial;

import java.util.ArrayDeque;

import com.github.rfqu.df4j.core.DataflowNode;
import com.github.rfqu.df4j.core.Task;

public class HelloWorld3 {
    static  class Collector extends DataflowNode {
        Input<String> input=new StreamInput<String>(new ArrayDeque<String>());
        StringBuilder sb=new StringBuilder();
        
        @Override
        // since dataflow node can have different number of inputs,
        // the act method have no parameters, values from inputs
        // has to be extracted manually.
        protected void act() {
            String message=input.get();
            if (message==null) {
                // StreamInput does not accept null values,
                // and null value signals that input is closed
                System.out.println(sb.toString());
            } else {
               sb.append(message).append(" ");
            }
        }
    }

    public static void main(String args[]) throws InterruptedException {
        Collector coll=new Collector();
        coll.input.send("Hello");
        coll.input.send("World 3");
        coll.input.close();
        Task.completeCurrentExecutorService();
    }
}
