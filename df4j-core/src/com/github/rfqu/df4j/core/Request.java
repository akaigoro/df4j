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
 * A message that carries callback port.
 * @param <T> actual type of Request (subclassed)
 * @param <R> type of result
 */
public class Request<T extends Request<T, R>, R> extends Link {
    protected Port<T> replyTo;
    protected R result;
    protected Throwable exc;

    public Request() {
    }

    public Request(Port<T> callback) {
        this.replyTo = callback;
    }

    /** initialize
     * @param replyTo destination
     */
    protected void prepare(Port<T> replyTo) {
        this.replyTo = replyTo;
        result = null;
        exc = null;
    }

    /** 
     * sends itself to the destination
     */
    @SuppressWarnings("unchecked")
    public void reply() {
        if (replyTo == null) {
            return;
        }
        replyTo.send((T) this);
    }

    /** sets the result and forwards to the destination
     * @param result
     */
    public void reply(R result) {
        this.result=result;
        reply();
    }

    /** sets the error and forwards to the destination
     * @param result
     */
    public void replyFailure(Throwable exc) {
        this.exc=exc;
        reply();
    }

    public Port<T> getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(Port<T> replyTo) {
        this.replyTo = replyTo;
    }

    public R getResult() {
        return result;
    }

    public void setResult(R result) {
        this.result = result;
    }

    public Throwable getExc() {
        return exc;
    }

    public void setExc(Throwable exc) {
        this.exc = exc;
    }
}
