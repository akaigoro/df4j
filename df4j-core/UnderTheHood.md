Что под капотом у java.util.concurrent.CompletableFuture?
---------------------------------------------------------

На самом деле там под капотом высокооптимизированный, трудночитаемый код, в котором разобраться мне не хватило терпения.
Я решил создать функциональный эквивалент на основе javadoc-спецификации, который бы был понятен и начинающему программисту. 
Глядя на этот  и поняв, что он в реальности делает,
 программист может легко понять, как ему использовать т.наз. Fluent API класса `CompletableFuture`.
 
 Этот функциональный эквивалент расположен в файле [CompletablePromise.java](https://github.com/akaigoro/df4j/blob/API-5/df4j-core/src/main/java/org/df4j/core/connector/messagescalar/CompletablePromise.java)
 
 Первые два метода Fluent API:
 
 ```java
    public static <U> CompletablePromise<U> supplyAsync(Supplier<U> supplier) {
        return supplyAsync(supplier, asyncExec);
    }

    public static <U> CompletablePromise<U> supplyAsync(Supplier<U> supplier,
                                                       Executor executor) {
        AsyncFunction<Void, U> asyncSupplier =  new AsyncFunction<>(supplier);
        asyncSupplier.start(executor);
        return asyncSupplier.asyncResult();
    }

```
Мы видим, что  `supplyAsync(Supplier<U> supplier)` сводится к вызову `supplyAsync(Supplier<U> supplier, Executor executor)`,
где `asyncExec` - некий дефолтный исполнитель, и  поэтому будем здесь и в дальнейшем рассматривать только полные версии
методов, с именами, кончающимися на _Async_ и последним параметром типа `Executor`.

итак, мы видим три действия, которые будут повторяться и в дальнейшем:

- создание объекта
