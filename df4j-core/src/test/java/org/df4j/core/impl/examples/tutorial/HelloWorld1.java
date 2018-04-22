package org.df4j.core.impl.examples.tutorial;

import org.df4j.core.impl.messagestream.Actor1;
import org.junit.Test;

public class HelloWorld1 {
    /**
     * collects strings
     * prints collected strings when argument is an empty string 
     */
     class Collector extends Actor1<String> {
         StringBuilder sb=new StringBuilder(); 
         
         @Override
         protected void act(String message) {
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
         coll.post("Hello");
         coll.post("World");
         coll.post("");
     }

}
