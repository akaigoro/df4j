package com.github.rfqu.df4j.nio.test;

import com.github.rfqu.df4j.nio.AsyncChannelFactory;
import com.github.rfqu.df4j.nio.AsyncChannelFactory1;


public class ConnectionTest1 extends ConnectionTest {
   {
       AsyncChannelFactory.setCurrentAsyncChannelFactory(new AsyncChannelFactory1());
   }
}