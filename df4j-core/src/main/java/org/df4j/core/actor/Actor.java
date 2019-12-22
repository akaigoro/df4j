package org.df4j.core.actor;

/**
 * {@link Actor} is a {@link Dataflow} with single {@link BasicBlock} which is executed in a loop.
 * In other words, Actor is a repeatable asynchronous procedure.
 * To handle streams of values efficiently, we need reusable nodes.
 *  The theory of asynchronous programming knows two kinds of reusable nodes: `Actors` and `Coroutines`.\
 *  Df4J has actors only, because coroutines require compiler support or bytecode transformations.
 *
 *   `Actors` here are <a href="https://pdfs.semanticscholar.org/2dfa/fb6ea86ac739b17641d4c4e51cc17d31a56f.pdf"><i>dataflow actors whith arbitrary number of parameters.</i></a>
 *   After processing the first set of arguments, the actor purges them out of parameters and waits until next set of arguments is ready.
 *
 *  The actor realm in df4j is located at the package {@link org.df4j.core.actor}. The base node class is {@link org.df4j.core.actor.Actor}.
 *  It extends {@link org.df4j.core.asyncproc.AsyncProc} and so has the default result and can contain one-shot parameters (which can be set only once).
 *  But to be fully functional, Actors must declare parameters capable to pass multiple tokens. They are declared in the same package:
 *  - Semahore: this is an asynchronous semaphore. It contains counter of permits. It is considered ready when the counter is greater than zero.
 *  After each round or Actor's execution, the counter is automatically decreased by 1.
 *  - StreamInput: a queue of references to objects.
 *
 *   Typical df4j actor is programmed as follows:
 *
 *   So the main difference is parameters which can keep a sequence of values.
 *   The node classes differ only that after calling the action procedure, the method {@link org.df4j.core.actor.Actor#start} is called again.
 *
 *   An interesting case is calling {@link org.df4j.core.actor.Actor#start} in an asynchronous callback like in
 *   {@link org.df4j.nio2.net.AsyncServerSocketChannel}.
 *
 *   The <a href="https://en.wikipedia.org/wiki/Actor_model"><i>Hewitt's actors</i></a> (e.g. <a href="https://akka.io/"><i>Akka</i></a>)
 *   with single predifined input parameter are trivial corner case of the dataflow actors.
 *   They are implemented by the class {@link org.df4j.core.actor.ext.Hactor}.
 *
 * <pre>{@code
 * public void testDFF() throws ExecutionException, InterruptedException {
 *      Function<Integer, Integer> square = arg -> arg * arg;
 *      BiFunction<Integer, Integer, Integer> plus = (argX, argY) -> argX + argY;
 *      // create nodes and connect them
 *      AsyncFunction<Integer, Integer> sqX = new AsyncFunction<>(square);
 *      AsyncFunction<Integer, Integer> sqY = new AsyncFunction<>(square);
 *      AsyncBiFunction<Integer, Integer, Integer> sum =
 *      new AsyncBiFunction<Integer, Integer, Integer>(plus);
 *      // make 2 connections
 *      sqX.subscribe(sum.param1);
 *      sqY.subscribe(sum.param2);
 *      // start all the nodes
 *      sqX.start();
 *      sqY.start();
 *      sum.start();
 *      // provide input information:
 *      sqX.post(3);
 *      sqY.post(4);
 *      // get the result
 *      int res = sum.asyncResult().get();
 *      Assert.assertEquals(25, res);
 * }
 * }</pre>
 * The same computation can be build using {@link java.util.concurrent.CompletableFuture}:
 * <pre>{@code
 *  public void testCF() throws ExecutionException, InterruptedException {
 *      Function<Integer, Integer> square = arg -> arg * arg;
 *      BiFunction<Integer, Integer, Integer> plus = (argX, argY) -> argX + argY;
 *      // create nodes and connect them
 *      CompletableFuture<Integer> sqXParam = new CompletableFuture();
 *      CompletableFuture<Integer> sqYParam = new CompletableFuture();
 *      CompletableFuture<Integer> sum = sqXParam
 *           .thenApply(square)
 *           .thenCombine(sqYParam.thenApply(square),
 *           plus);
 *      // provide input information:
 *      sqXParam.complete(3);
 *      sqYParam.complete(4);
 *      // get the result
 *      int res = sum.get();
 *      Assert.assertEquals(25, res);
 * }
 * }</pre>
 * However, the symmetry beween computation of `x^2` and `x^2` is lost.
 * Worst of all, nodes with more than 2 input connectors or with more than 1 output connector cannot be created at all!
 * This is the clear result of using fluent API instead of explicit object construction.
 *
 * The class [_CompletablePromise_](src/main/java/org/df4j/core/boundconnector/messagescalar/CompletablePromise.java) also provides
 * fluent API identical to that of _CompletableFuture_, only to demonstrate how a developer can live without it, or create his own.
 * Detailed explanation of that fluent API is in document [UnderTheHood](/UnderTheHood.md) (in Russian).

 */
public abstract class Actor extends AsyncProc {

    public Actor(Dataflow parent) {
        super(parent);
    }

    public Actor() {
        super();
    }

    @Override
    protected void run() {
        try {
            runAction();
            if (isCompleted()) {
                return;
            }
            this.awake(); // make loop
        } catch (Throwable e) {
            stop(e);
        }
    }

}
