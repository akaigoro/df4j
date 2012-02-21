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


/**
 * abstract node with several inputs and outputs
 */
public abstract class Function<R> {
    int gateCount=0;
    int readyGateCount=0;
    Connector<R> res=new Connector<R>();;

    public class Input<T> implements Port<T> {
        {gateCount++;}
        public T operand;
        protected boolean opndready = false;

        @Override
        public void send(T value) {
            if (opndready) {
                throw new IllegalStateException("illegal send");
            }
            synchronized (this) {
                operand = value;
                opndready = true;
                readyGateCount++;
                if (readyGateCount<gateCount) {
                    return;
                }
            }
            fire();       
        }
    }

    public void setRes(R val) {
    	res.send(val);
    }

    public Connector<R> getConnector() {
		return res;
	}

    public void connect(Port<R> sink) {
        res.connect(sink);
    }
    
    public void connect(Port<R>... sink) {
        res.connect(sink);
    }
    
    protected abstract void fire();
    
}