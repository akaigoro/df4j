/**
 * non-repeatable asynchronous procedures - The Scalar realm.

 The Scalar realm contains one-shot events and asynchronous procedure calls to handle them. In Java SE, it is represented by CompletableFuture class.
 Df4j has alternative implementation, interoperable with CompletableFuture - this the class org.df4j.core.asyncproc.AsyncProc.
 AsyncProc has no fluent API which CompletableFuture has, and so the code could take more lines of the programming text.
 On the other hand, user can create things that are out of the scope of that fluent API, for example, asynchronouus procedures with more than 2 parameters.

 To create typical one-shot asynchronous procedure in Df4j, user has to extend class org.df4j.core.asyncproc.AsyncProc, declare one or more input parameters,
 and override the the computational method `run()`.
 The class AsyncProc declares default output parameter named "result", but additional output parameters also can be declared.

 For example, let's create an async computation to compute value of x^2+y^2, each arithmetic operation computed in its own asynchronous procedure call.

 First, we need to create 2 classes, one to compute the square of a value, and second to compute the sum.

 ```java
 public static class Square extends AsyncProc<Integer> {
 final ScalarInput<Integer> param = new ScalarInput<>(this);

 public void run() {
 Integer arg = param.current();
 int res = arg*arg;
 result.onComplete(res);
 }
 }
 ```
 Note the constructor of a bound parameter has additional parameter - a reference to the parent node (this).
 This is to enable parameters to notify the body when they peceive arguments. Body will start execution when all the parameters are ready.

 ```java
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
 ```

 Now we can create the dataflow graph consisting of 3 nodes, pass arguments to it and get the result:

 ```java
 public class SumSquareTest {

@Test
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
 ```
 All the nodes in this graph execute once and cannot be reused.

 */
package org.df4j.core.asyncproc;
