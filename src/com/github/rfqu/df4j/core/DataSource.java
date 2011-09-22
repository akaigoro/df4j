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
 * 
 * A kind of dataflow variable: single input, multiple asynchronous outputs.
 * 
 * @param <R>
 *                type of result
 */
public class DataSource<R> implements Port<R> {
    private MessageQueue<Request<R>> requests = new MessageQueue<Request<R>>();
    private volatile R result;

    public DataSource<R> send(R result) {
        this.result = result;
        for (;;) {
            Request<R> request;
            synchronized (this) {
                request = requests.poll();
                if (request == null) {
                    return this;
                }
            }
            try {
                request.reply(result);
            } catch (Exception e) {
                failure(request, e);
            }
        }
    }

    /**
     * handles the failure
     * 
     * @param message
     * @param e
     */
    protected void failure(Request<R> request, Exception e) {
        e.printStackTrace();
    }

    public <S extends Port<R>> S request(S sink) {
        if (result != null) {
            sink.send(result);
        }
        synchronized (this) {
            if (result == null) {
                requests.enqueue(new Request<R>(sink));
                return sink;
            }
        }
        sink.send(result);
        return sink;
    }

    public void request(Request<R> request) {
        if (result != null) {
            request.reply(result);
        }
        synchronized (this) {
            if (result == null) {
                requests.enqueue(request);
                return;
            }
        }
        request.reply(result);
    }

}
