/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.examples;

import java.io.PrintStream;

import org.junit.Test;

import com.github.rfqu.df4j.core.Actor;

public class HelloWorld {
    PrintStream out = System.out;
    
    class Collector extends Actor<String> {
        StringBuilder sb=new StringBuilder();
        
        @Override
        protected void act(String message) throws Exception {
            if (message.length()==0) {
                out.println(sb.toString());
                out.flush();
            } else {
                sb.append(message);
                sb.append(" ");
            }
        }
        
    }

    @Test
    public void test1() throws InterruptedException {
        Collector coll=new Collector();
        coll.send("Hello");
        coll.send("World");
        coll.send("");
    }

    public static void main(String args[]) throws InterruptedException {
        HelloWorld nt = new HelloWorld();
        nt.test1();
    }

}
