Что под капотом у java.util.concurrent.CompletableFuture?
---------------------------------------------------------

На самом деле там под капотом высокооптимизированный, трудночитаемый код, в котором разобраться мне не хватило терпения.
Я решил создать функциональный эквивалент на основе javadoc-спецификации, который бы был понятен и начинающему программисту. 
Глядя на этот эквивалент и поняв, что он в реальности делает, программист может легко понять, 
как ему использовать многочисленные методы _Fluent API_ класса `CompletableFuture`.
 
Этот функциональный эквивалент расположен в файле [CompletablePromise.java](https://github.com/akaigoro/df4j/blob/API-5/df4j-core/src/main/java/org/df4j/core/connector/messagescalar/CompletablePromise.java)
 
 Первые два метода Fluent API:
 
 ```java
    public static <U> CompletablePromise<U> supplyAsync(Supplier<U> supplier) {
        return supplyAsync(supplier, asyncExec);
    }

    public static <U> CompletablePromise<U> supplyAsync(Supplier<U> supplier,
                                                       Executor executor) {
        AsyncResult<U> asyncSupplier =  new AsyncResult<>(supplier);
        asyncSupplier.start(executor);
        return asyncSupplier.asyncResult();
    }

```
Мы видим, что  `supplyAsync(Supplier<U> supplier)` сводится к вызову `supplyAsync(Supplier<U> supplier, Executor executor)`,
где `asyncExec` - некий дефолтный исполнитель, и  поэтому будем здесь и в дальнейшем рассматривать только полные версии
методов, с именами, кончающимися на _Async_ и последним параметром типа `Executor`.

итак, мы видим три действия, которые будут повторяться и в дальнейшем:

- создание объекта типа  `AsyncResult`. Это асинхронная процедура без параметров и одним асинхронным результатом.
- направление его на заданный исполнитель
- возврат объекта - асинхронного результата.

Поскольку параметров у этой асинхронной процедуры нет, она будет исполняться без задержки 
(асинхронные процедуры с параметрами ждут заполнения всех параметров и только затем передаются исполнителю).

Асинхронный результат типа `CompletablePromise` - это самая интересная часть рассказа. 
Как мы знаем, `CompletableFuture` реализует интерфейс `java.util.concurrent.Future`, и с его помощью другие потоки могут
дождаться результата асинхронного вычисления, вызывая метод `Future.get()`. 
Но этот метод может вызывать блокировку текущего потока и поэтому неприменим в асинхронных процедурах.
Асинхронные процедуры обмениваются результатами по другому интерфейсу, который, к сожалению, не входит в число публичных.
Его функциональным эквивалентом является интерфейс [ScalarPublisher](https://github.com/akaigoro/df4j/blob/API-5/df4j-core/src/main/java/org/df4j/core/connector/messagescalar/ScalarPublisher.java)
и который в других асинхронных библиотках иногда называется _Promise_.
Его использование проследим на сдедующем примере:

```java
    public <U> CompletablePromise<U> thenApplyAsync(
            Function<? super T,? extends U> fn, Executor executor) {
        AsyncFunction<? super T,? extends U> asyncFunc =  new AsyncFunction<>(fn);
        this.subscribe(asyncFunc);
        asyncFunc.start(executor);
        return (CompletablePromise<U>) asyncFunc.asyncResult();
    }

```
Здесь все то же самое, как и в `supplyAsync`, но создаваемая аинхронная функция уже имеет один параметр, и надо позаботится, чтобы туда рано или поздно были переданы данные.
Это делается в строчке
```java
        this.subscribe(asyncFunc);
```
 которую следует понимать как сокращение для
```java
        this.subscribe(asyncFunc.argument);
```
где `argument` - это тот самый единственный аргумент асинхронной функции.
Смысл такого соединения в том, что когда текущий объект, обозначенный 'this', получит значение, это значение передастся в `asyncFunc.argument`
и всем остальным подписчикам, если таковые появятся. Такое соединение не блокирует потоки. 

Следующий пример практически повторяет предыущий:

```java
    public CompletablePromise<Void> thenAcceptAsync(Consumer<? super T> action,
                                                   Executor executor) {
        AsyncFunction<T,Void> asyncConsumer =  new AsyncFunction<>(action);
        this.subscribe(asyncConsumer);
        asyncConsumer.start(executor);
        return asyncConsumer.asyncResult();
    }
```
 
Единственная разница в том, что в построитель передается аргумент типа `Consumer`, который описывает методы с оним параметром, не возвращающие результата (типа `void`).
Какой же тогда результат нам предоставляет создаваемая асинхронная процедура? Это видно в описании построителя: `CompletablePromise<Void>`. 
Реальный результат будет типа `Void`, и единственным значением этого типа является `null`. 
Нужен же этот результат только для того, чтобы отследить момент завершения вычисления создаваемой асинхронной процедуры - точно так же, как для потоков
мы используем метод `Thread.join()`.

Вообще параллелей между многопоточным и асинхронным программированием очень много.
Можно взять любой способ обмена данными между потоками и построить для него асинхронный аналог. 
Вот мы только что рассмотрели асинхронный `CompletablePromise` - асинхронный аналог `CompletableFuture`.
Для `java.util.concurrent.Semaphore` аналогом является [Semafor](https://github.com/akaigoro/df4j/blob/API-5/df4j-core/src/main/java/org/df4j/core/connector/permitstream/Semafor.java),
для `java.util.concurrent.BlockingQueue` -  [PickPoint](https://github.com/akaigoro/df4j/blob/API-5/df4j-core/src/main/java/org/df4j/core/node/messagestream/PickPoint.java).

Важно отметить, что как один поток может по очереди использовать разные методы обмена данными, так и одна асинхронная процедура может использовать разные методы,
но не по очереди, а одновременно, заявив в своем описании коннекторы разных типов. 
При этом соединятся могут только выходные коннекторы одного типа со входными коннекторами того же типа.
 
