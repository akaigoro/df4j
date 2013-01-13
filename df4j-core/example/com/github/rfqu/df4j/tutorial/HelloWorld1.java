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

import com.github.rfqu.df4j.core.Actor;
import com.github.rfqu.df4j.core.DFContext;

public class HelloWorld1 {
    static class Collector extends Actor<String> {
        StringBuilder sb=new StringBuilder();
        
        @Override
        protected void act(String message) {
            if (message.length()==0) {
                System.out.println(sb.toString());
            } else {
                sb.append(message).append(" ");
            }
        }        
    }

    public static void main(String args[]) {
        Collector coll=new Collector();
        coll.post("Hello");
        coll.post("World 1");
        coll.post("");
        DFContext.completeCurrentExecutorService();
    }
}
