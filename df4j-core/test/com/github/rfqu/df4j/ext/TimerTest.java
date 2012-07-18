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

import java.util.concurrent.ExecutionException;

import org.junit.Test;

import com.github.rfqu.df4j.core.Port;
import com.github.rfqu.df4j.ext.Timer;

public class TimerTest {
	long start=System.currentTimeMillis();

    class Portik<T> implements Port<T> {
		@Override
		public void send(T m) {
			long elapsed = System.currentTimeMillis()-start;
            System.out.println("received "+m.toString()+" at "+elapsed);
		}
    }

    class Act implements Runnable {
        int value;

        public Act(int value) {
            this.value = value;
        }

        @Override
        public void run() {
            long elapsed = System.currentTimeMillis()-start;
            System.out.println("act "+value+" at "+elapsed);
        }
    }
    
    @Test
    public void scheduleTest2() throws InterruptedException, ExecutionException {        
        Timer timer= Timer.getCurrentTimer();
        timer.schedule(new Act(112), 112);
        timer.schedule(new Act(24), 24);
        timer.schedule(new Act(1), 1);
        timer.schedule(new Act(2), 2);
        timer.schedule(new Act(10), 10);
        timer.schedule(new Act(57), 57);
        System.out.println("about to shut down");
        timer.shutdown().get();
        System.out.println("shut down");
    }

    @Test
    public void scheduleTest3() throws InterruptedException, ExecutionException {
        Portik<Integer> portik=new Portik<Integer>();
        Timer timer= Timer.getCurrentTimer();
        timer.schedule(portik, 112, 112);
        timer.schedule(portik, 24, 24);
        timer.schedule(portik, 1, 1);
        timer.schedule(portik, 2, 2);
        timer.schedule(portik, 10, 10);
        timer.schedule(portik, 57, 57);
        System.out.println("about to shut down");
        timer.shutdown().get();
        System.out.println("shut down");
    }

    public static void main(String args[]) throws InterruptedException, ExecutionException {
        TimerTest nt = new TimerTest();
        nt.scheduleTest2();
        nt.scheduleTest3();
    }

}
