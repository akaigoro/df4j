/**
 * actors: repeatable asynchronous procedures - the Actor realm
 *
 To handle streams of values efficiently, we need reusable nodes.
 The theory of asynchronous programming knows two kinds of reusable nodes: `Actors` and `Coroutines`.
 Df4J has actors only. because coroutines require compiler support or bytecode transformations.

 The actor realm in df4j is located at the package org.df4j.core.actor. The base node class is org.df4j.core.actor.Actor.
 It extends AsyncProc and so has the default result and can contain one-shot parameters (which can be set only once).
 But to be fully functional, Actors must declare parameters capable to pass multiple tokens.
 These are declared in the same package:
 - Semahore: this is an asynchronous semaphore. It contains conter of permits. It is considered ready when the counter is greater than zero.
 After each round or Actor's execution, the counder is automatically decreased by 1.
 - StreamInput: a queue of references to objects.

 `Actors` here are [dataflow actors whith arbitrary number of parameters] (https://pdfs.semanticscholar.org/2dfa/fb6ea86ac739b17641d4c4e51cc17d31a56f.pdf).
 After processing the first set of arguments, the actor purges them out of parameters and waits until next set of arguments is ready.
 The [Hewitt's actors](https://en.wikipedia.org/wiki/Actor_model) (e.g. [Akka](https://akka.io/)) with single predifined input parameter
 are trivial corner case of the dataflow actors. They are implemented in class class is org.df4j.core.actor.ext.Hactor.

 Typical df4j actor is programmed as follows:

 So the main difference is parameters which can keep a sequence of values. The node classes differ only that after calling the action procedure,
 the method `AsyncTask.start()` is called again.



 An interesting case is calling `start()` in an asynchronous callback like in
 [AsyncServerSocketChannel](../df4j-nio2/src/main/java/org/df4j/nio2/net/AsyncServerSocketChannel.java).

 To effectively use Actors, they should declare stream parameters.

 For example. let's construct an Actor which computes the module (size) of vectors represented by their coordinates x and y:

 The dataflow graph can have cycles. The connections through the cycles must allow transfer of multiple tokens.

 For simplicity, only two kinds of connection interfaces are used: for black and for colored tokens.
 Implementations which allow single tokens and that which allow streams of tokens, use the same interfaces. Intrefaces for colored tokens are borrowed from
 the Project Reactor (https://projectreactor.io/).

 In practice most async libraries tries to oversymplify and do not allow to create parameters separately from the body of async procedure.
 The result is overcomplicated API, which simultanousely is limited in expressiveness.

 Using it, the code can be more compact, but we want to demonstrate the general plan to build asynchronous executions:

 - create nodes
 - connect output connectors with input connectors
 - start nodes
 - provide input information
 - take computed result(s)

 Actually, the order of these steps can vary.

 We can avoid creating new node classes, if our computation procedures are of standard java functional types like `Function`,
 `BiFunction` etc:

 ```java
 @Test
 public void testDFF() throws ExecutionException, InterruptedException {
 Function<Integer, Integer> square = arg -> arg * arg;
 BiFunction<Integer, Integer, Integer> plus = (argX, argY) -> argX + argY;
 // create nodes and connect them
 AsyncFunction<Integer, Integer> sqX = new AsyncFunction<>(square);
 AsyncFunction<Integer, Integer> sqY = new AsyncFunction<>(square);
 AsyncBiFunction<Integer, Integer, Integer> sum =
 new AsyncBiFunction<Integer, Integer, Integer>(plus);
 // make 2 connections
 sqX.subscribe(sum.param1);
 sqY.subscribe(sum.param2);
 // start all the nodes
 sqX.start();
 sqY.start();
 sum.start();
 // provide input information:
 sqX.post(3);
 sqY.post(4);
 // get the result
 int res = sum.asyncResult().get();
 Assert.assertEquals(25, res);
 ```
 The same computation can be build using `java.util.concurrent.CompletableFuture`:

 ```java
 @Test
 public void testCF() throws ExecutionException, InterruptedException {
 Function<Integer, Integer> square = arg -> arg * arg;
 BiFunction<Integer, Integer, Integer> plus = (argX, argY) -> argX + argY;
 // create nodes and connect them
 CompletableFuture<Integer> sqXParam = new CompletableFuture();
 CompletableFuture<Integer> sqYParam = new CompletableFuture();
 CompletableFuture<Integer> sum = sqXParam
 .thenApply(square)
 .thenCombine(sqYParam.thenApply(square),
 plus);
 // provide input information:
 sqXParam.complete(3);
 sqYParam.complete(4);
 // get the result
 int res = sum.get();
 Assert.assertEquals(25, res);
 }
 ```

 However, the symmetry beween computation of `x^2` and `x^2` is lost.
 Worst of all, nodes with more than 2 input connectors or with more than 1 output connector cannot be created at all!
 This is the clear result of using fluent API instead of explicit object construction.

 The class [_CompletablePromise_](src/main/java/org/df4j/core/boundconnector/messagescalar/CompletablePromise.java) also provides
 fluent API identical to that of _CompletableFuture_, only to demonstrate how a developer can live without it, or create his own.
 Detailed explanation of that fluent API is in document [UnderTheHood](/UnderTheHood.md) (in Russian).


 */
package org.df4j.core.actor;
