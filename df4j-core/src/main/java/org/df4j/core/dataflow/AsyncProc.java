/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.core.dataflow;

import org.df4j.protocol.Completable;

import java.util.concurrent.Executor;

/**
 * {@link AsyncProc} is a {@link Dataflow} with single {@link BasicBlock} which is executed only once.
 *
 * It consists of asynchronous connectors, implemented as inner classes,
 * user-defined asynchronous procedure, and a mechanism to call that procedure
 * using supplied {@link Executor} as soon as all connectors are unblocked.
 *
 * An asynchronous procedure is a kind of parallel activity, along with a Thread.
 * It differs from a thread so that while waiting for input information to be delivered,
 * it does not use procedure stack and so does not wastes core memory.
 * As a result, we can manage millions of asynchronous procedures, while 10000 threads is already a heavy load.
 * This can be important, for example, when constructing a web-server.
 *
 * An asynchronous procedure differs from ordinary synchronous procedure in the following way:
 *  - it resides in the heap and not on a thread's stack
 *  - its input and output parameters are not simple variables but separate objects in the heap
 *  - it starts execution when all its parameters are ready
 *  - it executes not on the caller's thread, but on the Executor, assigned to the procedure at the moment of creation.
 *
 * So: parameter objects are tightly connected to the asynchronous procedure object,
 * and parameters cooperate to detect the moment when all the parameters are ready, and then submit the procedure to the executor.
 *
 * Input parameters are connected to output parameters of other procedures.
 * As a result, the whole program is represented as a (dataflow) graph.
 * Each parameter implements some connecting protocol. Connected parameters must belong to the same protocol.
 * In Java SE, asynchronous procedure is represented by CompletableFuture class.
 Df4j has alternative implementation, interoperable with CompletableFuture - this the class {@link AsyncProc}.
 AsyncProc has no fluent API which CompletableFuture has, and so the code could take more lines of the programming text.
 On the other hand, user can create things that are out of the scope of that fluent API, for example, asynchronouus procedures with more than 2 parameters.

 To create typical one-shot asynchronous procedure in Df4j, user has to extend class {@link AsyncProc}, declare one or more input parameters,
 and override the the computational method {@link AsyncProc#runAction()} .

 For example, let's create an async computation to compute value of x^2+y^2, each arithmetic operation computed in its own asynchronous procedure call.
 First, we need to create 2 classes, one to compute the square of a value, and second to compute the sum.

 <pre>{@code
public static class Square extends AsyncProc<Integer> {
final ScalarInput<Integer> param = new ScalarInput<>(this);

public void run() {
Integer arg = param.current();
int res = arg*arg;
result.onComplete(res);
}
}
}</pre>
 Note the constructor of a bound parameter has additional parameter - a reference to the parent node (this).
 This is to enable parameters to notify the body when they peceive arguments. Body will start execution when all the parameters are ready.

 <pre>{@code
public static class Sum extends AsyncProc<Integer> {
final ScalarInput<Integer> paramX = new ScalarInput<>(this);
final ScalarInput<Integer> paramY = new ScalarInput<>(this);

public void run() {
Integer argX = paramX.current();
Integer argY = paramY.current();
int res = argX + argY;
result.onSuccess(res);
}
}
}</pre>

 Now we can create the dataflow graph consisting of 3 nodes, pass arguments to it and get the result:

 <pre>{@code
public class SumSquareTest {
public void testAP() throws ExecutionException {
// create nodes
Square sqX = new Square();
Square sqY = new Square();
Sum sum = new Sum();
// make 2 connections
sqX.result.subscribe(sum.paramX);
sqY.result.subscribe(sum.paramY);
// provide input information:
sqX.param.onSuccess(3);
sqY.param.onSuccess(4);
// get the result
int res = sum.asyncResult().get(1, TimeUnit.SECONDS);
Assert.assertEquals(25, res);
}
}
}</pre>
 All the nodes in this graph execute once and cannot be reused.
*/
public abstract class AsyncProc extends BasicBlock implements Completable.Source, Activity {

    public AsyncProc(Dataflow parent) {
        super(parent);
    }

    public AsyncProc() {
        super(new Dataflow());
    }

    @Override
    public void start() {
        super.awake();
    }

    @Override
    public boolean isAlive() {
        return super.dataflow.isAlive();
    }

    @Override
    public void subscribe(Completable.Observer co) {
        dataflow.subscribe(co);
    }

    public boolean isCompleted() {
        return dataflow.isCompleted();
    }

    public void join() {
        dataflow.join();
    }

    public boolean blockingAwait(long timeout) {
        return dataflow.blockingAwait(timeout);
    }

    protected void run() {
        try {
            runAction();
            stop();
        } catch (Throwable e) {
            stop(e);
        }
    }

}