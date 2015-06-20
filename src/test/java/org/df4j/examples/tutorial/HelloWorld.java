package org.df4j.examples.tutorial;

import org.df4j.core.Actor;
import org.junit.Test;

public class HelloWorld {
    /**
     * collects strings
     * prints collected strings when argument is an empty string 
     */
     class Collector extends Actor {
         StreamInput<String> input=new StreamInput<String>(); // actor's parameter
         StringBuilder sb=new StringBuilder(); 
         
         @Override
         protected void act() {
             String message=input.get();
             // empty string indicates the end of stream
             // nulls are not allowed
             if (message=="") {
                 System.out.println(sb.toString());
                 sb.setLength(0);
             } else {
                sb.append(message);
                sb.append(" ");
             }
         }
     }

     @Test
     public void test() {
         Collector coll=new Collector();
         coll.input.post("Hello");
         coll.input.post("World");
         coll.input.post("");
     }

}
