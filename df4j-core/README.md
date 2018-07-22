Everything should be made as simple as possible, but not simpler. - Albert Einstein
------------------- 

How to implement an asynchronous procedure.
------------------------------------------
An asynchronous procedure differs from a thread that while waiting for input information to be delivered, 
it does not use procedure stack and so does not wastes core memory. 
As a result, we can manage millions of asynchronous procedures,
while 10000 threads is already a heavy load. This can be important, for example, when constructing a web-server.

To build an asynchronous procedure, first we need is to move parameters out of procedure stack to the heap.
Second, we need to build an object which calls requred procedure as soon as all the arguments are received.

That's it.

Create an object which knows which ordinary procedure to call, and bound asynchronous paramerters to it.

Sounds simple, but in practice most async libraries tries to oversymplify and do not allow to create parameters separately from
the async procedure. The result is overcomplicated API, which simultanousely is limited in expressiveness.

For example, let's create an async computation to compute value of x^2+y^2, each arithmetic operation computed in its own asynchronous procedure call.

First, we need to create 2 classes, one to compute a square of a value, and second to compute the sum.


```java
public class Square extends AsyncProc {
    final CompletablePromise<Double> result = new CompletablePromise<>();
    final ScalarInput<Double> param = new ScalarInput<>(this);

    @Action
    public void compute(Double arg) {
      double res = arg*arg;
      result.complete(res);
    }
}
```
Here we see a node with one output connector `result` and one input connector `param` bound to the node.
Note the bound connectors have additional parameter - a reference to the parent node.

The magic behind the annotation `@Action` calls the annotated method with an argument, extracted from all the bound parameters.

```java
public class Sum extends AsyncProc {
    final CompletablePromise<Double> result = new CompletablePromise<>();
    final ScalarInput<Double> paramX = new ScalarInput<>(this);
    final ScalarInput<Double> paramY = new ScalarInput<>(this);

    @Action
    public void compute(Double argX, Double argY) {
      double res = argX + argY;
      result.complete(res);
    }
}
```
Here we see an async proc with 2 parameters and one result.

Now we can create the dataflow graph, pass arguments to it and get the result:

```java
public class SumSquareTest {

    @Test
    public void test() throws ExecutionException, InterruptedException {
        // create 3 nodes
        Square sqX = new Square();
        Square sqY = new Square();
        Sum sum = new Sum();
        // make 2 connections
        sqX.result.subscribe(sum.paramX);
        sqY.result.subscribe(sum.paramY);
        // start all the nodes
        sqX.start();
        sqY.start();
        sum.start();
        // provide input information:
        sqX.param.post(3);
        sqY.param.post(4);
        // get the result
        int res = sum.result.get();
        Assert.assertEquals(25, res);
    }
}
``` 

 The library also has a node class `AsyncFunc` which is an `Asynctask` with predefined output connector `result`.
 Using it, the code can be more compact, but we want to demonstrate the general plan to build asynchronous executions:
 
 - create nodes
 - connect output connectors with input connectors
 - start nodes
 - provide input information
 - take computed result(s)
 
Actually, the order of these steps can vary.

We can avoid creating new node classes, if out computation procedures are of standard java functional types like `Function`, 
`biFunction` etc:

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
 

What are Actors compared to Asycnchronous Procedures?
----------------------------------------------------
`Actors` here are both [Hewitt's actors](https://en.wikipedia.org/wiki/Actor_model) (e.g. [Akka](https://akka.io/)) 
with single predifined input parameter, and dataflow actors whith arbitrary number of parameters. 
In short, actors are repeatable asynchronous procedures. 
After processing first set of arguments, they purge them out of parameters and wait until next set of arguments is ready.
So the main difference is parameters which can keep a sequence of values. The node classes differ only that after calling the action procedure,
the method `AsyncTask.start()` is called again. 
The node class even can be [AsyncProc](src/main/java/org/df4j/core/tasknode/AsyncProc.java) itself, with method `AsyncProc::start()`
called by a user-defined method. 
An interesting case is calling `start()` in an asynchronous callback like in
 [AsyncServerSocketChannel](../df4j-nio2/src/main/java/org/df4j/nio2/net/AsyncServerSocketChannel.java).   

Supported protocols.
-------------------
In the above example which compute expression `x^2+x^2` all connectors used the **scalar message** protocol, 
which consists of 2 steps:
- connection established: `publisher.subscribe(subscriber); subscriber.onSubscribe(subscription);`
- a value is passed and the connection is closed: `subscriber.post(value);`.

Connectors for this interface are located in the  package [connector/messagescalar](src/main/java/org/df4j/core/boundconnector/messagescalar). 
Nodes that support only scalar connectors are located in the  package [node/messagescalar](src/main/java/org/df4j/core/tasknode/messagescalar). 
The subsequent subscriptions of the same or other subscribers can receive the same or different values. 
In the above example, all subscribers receive the same value, and this is natural, 
because the value is the result of concrete calculation. 
But publishers which provide different values for different connections can easily be implemented. 
One of such publishers is  [PickPoint](src/main/java/org/df4j/core/tasknode/messagestream/PickPoint.java). 
It receives stream of messages and delivers each message to single subscriber.
It is asynchronous analogue of `java.util.concurrent.BlockingQueue`.
It even implements the `BlockingQueue` interface, and so it can connect both threads and asynchronous procedures in all combinations.
On the input side it uses **message stream** protocol, which consists of 3 steps:

- connection established: `publisher.subscribe(subscriber); subscriber.onSubscribe(subscription);`
- arbitrary number of values is passed to the same subscriber: `subscriber.post(value);`.
- the connection is closed by explicit request from publisher side: `subscriber.complete(value);`,
 or from the subscriber's side: 'subscription.cancel();'. 

Connectors for this interface are located in the  package [connector/messagestream](src/main/java/org/df4j/core/boundconnector/messagestream). 
Nodes that support both scalar and stream connectors are located in the 
package [node/messagestream](src/main/java/org/df4j/core/tasknode/messagestream). 

The protocol **permit stream** is the same as **message stream** protocol,
 but transmited tokens does not carry any value and are indistinguishable.
In the synchronous world, the connector for this protocol is `java.util.concurrent.Semaphore`. 
Its asynchronous counterpart, [Semafor](src/main/java/org/df4j/core/boundconnector/permitstream/Semafor.java),
is a bound asynchronous connector and, as any other bound connector (parameter),
prevents the submission the bounded asynchronous procedure to the executor until its internal counter become positive. 
After the submission to the executor, the counter is automatically decreased by 1.

The last protocol, implemented currently in the **df4j** library is **reactive stream**.
Basically, its interfaces are roughly equivalent to the interfaces declared in the class `java.util.concurrent.Flow` in Java9.
What is interesting of this protocol, is that its connectors are implemented as a pair of connectors of lower level protocols:
 **permit stream** and **message stream**, and **permit stream** connectors are configured to work in opposite direction than **message stream**.
