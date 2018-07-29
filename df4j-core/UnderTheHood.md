Реконструкция методов java.util.concurrent.CompletableFuture
------------------------------------------------------------

Для чего нужна реконструкция, если исходный код этого класса открыт?

На самом деле там под капотом высокооптимизированный, трудночитаемый код, изучение которго мало что дает в педагогическом плане. 
Поэтому мы воссоздадим семантику операций по их спецификациям, и напишем функционально эквивалентный, понятный и читаемый код,
хотя возможно, не самый экономный по расходу памяти и времени процессора.

Начнем с относительно простого метода: 
 ```java
public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier,
                                                   Executor executor)
Returns a new CompletableFuture that is asynchronously completed by a task running in the given executor with the value obtained by calling the given Supplier.
Type Parameters:
U - the function's return type
Parameters:
supplier - a function returning the value to be used to complete the returned CompletableFuture
executor - the executor to use for asynchronous execution
Returns:
the new CompletableFuture
```
Прочитаем внимательно спецификацию:
```java
Returns a new CompletableFuture
```
То есть, создается объект типа `CompletableFuture` либо его подкласса и возвращается в качестве результата.
```java
that is asynchronously completed by a task running in the given executor`
```
Кроме того, создается задача, исполняемая на `Executor`'e. 
Как мы знаем, `Executor`'ы принимают только объекты типа `Runnable`. 
Runnable это интерфейс, и первый объект вполне может его реализовывать - так мы совместим две функции в одном объекте.
```java
 completed ... with the value obtained by calling the given Supplier.
```

Этот `Runnable` должен вызвать данный `Supplier` и полученным значением завершить созданный `CompletableFuture`.
`Supplier` - это функция без параметров, так что закодировать все это очень просто:

```java
    class CompletableFutureForSupplyAsync<U> extends CompletableFuture<U> implements Runnable {
        Supplier<U> supplier;

        public CompletableFutureForSupplyAsync(Supplier<U> supplier) {
            this.supplier = supplier;
        }

        public void run() {
            try {
                U result = supplier.get();
                super.complete(result);
            } catch (Throwable e) {
                super.completeExceptionally(e);
            }
        }
    }

    public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier,
                                                       Executor executor) {
        CompletableFutureForSupplyAsync<U> task = new CompletableFutureForSupplyAsync<>(supplier);
        executor.execute(task);
        return task;
    }
```

Следующий пример несколько сложнее:
```java
public <U> CompletableFuture<U> thenApplyAsync(Function<? super T,? extends U> fn,
                                               Executor executor)
Returns a new CompletionStage that, when this stage completes normally, is executed using the supplied Executor,
  with this stage's result as the argument to the supplied function. 
  See the CompletionStage documentation for rules covering exceptional completion.
Specified by:
thenApplyAsync in interface CompletionStage<T>
Type Parameters:
U - the function's return type
Parameters:
fn - the function to use to compute the value of the returned CompletionStage
executor - the executor to use for asynchronous execution
Returns:
the new CompletionStage

```

`Returns a new CompletionStage that... is executed using the supplied Executor`

Здесь нам прямо предлагают оформить создаваемый объект оформить в виде `Runnable`.

 `... with this stage's result as the argument to the supplied function.` 
 
 а вот это уже интереснее. Передаваемая нам функция имеет параметр, и значением этого параметра служит значение,
 завершающее текущий `CompletionStage`. В момент вызова `thenApplyAsync` это значение может быть неизвестно, поэтому
 сразу запустить задачу на `Executor` мы не можем. Вместо этого мы должны договориться с текущим `CompletionStage`,
 чтобы в момент своего завершения она передала свое значение в задачу.
 Среди многочесленных методов `CompletionStage` имеются один, в точности подходщий для этой цели, это `whenComplete`:
 
 ```java
public CompletableFuture<T> whenComplete(BiConsumer<? super T,? super Throwable> action)
Returns a new CompletionStage with the same result or exception as this stage,
 that executes the given action when this stage completes.
```

То есть, во вновь создаваемом объекте-задаче достаточно реализовать еще интерфейс `BiConsumer` для приема аргумента:
```java
     class CompletableFutureForApplyAsync<T, U> extends CompletableFuture<U>
            implements Runnable, BiConsumer<T,Throwable>
    {
        Function<? super T,? extends U> fn;
        Executor executor;
        T arg;
        Throwable throwable;

        public CompletableFutureForApplyAsync(Function<? super T,? extends U> fn, Executor executor) {
            this.fn = fn;
            this.executor = executor;
        }

        @Override   // implementation of BiConsumer interface
        public void accept(T argument, Throwable throwable) {
            if (throwable != null) {
                this.throwable = throwable;
            } else {
                this.arg = argument;
            }
            executor.execute(this);
        }

        @Override
        public void run() {
            if (throwable == null) {
                try {
                    U result = fn.apply(arg);
                    super.complete(result);
                } catch (Throwable e) {
                    super.completeExceptionally(e);
                }
            } else {
                super.completeExceptionally(throwable);
            }
        }
    }

    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T,? extends U> fn,
                                                   Executor executor
    ) {
        CompletableFutureForApplyAsync<T,U> task = new CompletableFutureForApplyAsync<>(fn, executor);
        this.whenComplete(task);
        return task;
    }
}
```
Этот пример очень важен для понимания природы асинзронного пронраммирования, поэтому еще раз перечислим его основные шаги:

1) создается асинхронная процедура:
```java
     CompletableFutureForApplyAsync<T,U> task = new CompletableFutureForApplyAsync<>(fn, executor);
``` 
2) она пока не готова к исполнению, поэтому мы просим поставщика недостающего аргумента передать нам этот аргумент в будущем,
вызвав поданный нами метод:
```java
       this.whenComplete(task);
```
3) в этом методе мы не только сохраняем полученный аргумент, но и запускаем задачу на исполнение (см метод `accept`()).

4) исполнение задачи сводится к выполнению поданой нам функции и сохранении результата. 
Этот реультат может быть точно также затребован другими процедурами с помощью метода `whenComplete`(),
примененного уже к нашему вновь построенному объекту, 
так что мы можем построить цепочку асинхронных процедур произвольной длины.
Но исполнятся эта цепочка будет строго последовательно, без всякого параллелизма. 

A как же изобразить более сложную диаграмму вычислений, содержащую параллельные ветви? 
Для этого служит метод `thenCombine(Async)`. 
Если в предыдущем примере мы запускали асинхронную процедуру с одним аргументом, то в этом - с двумя.
При этом вычисление обоих аргументов может происходить параллельно.
```java
ublic <U,V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other,
                                                   BiFunction<? super T,? super U,? extends V> fn,
                                                   Executor executor)
Description copied from interface: CompletionStage
Returns a new CompletionStage that, when this and the other given stage complete normally,
is executed using the supplied executor, with the two results as arguments to the supplied function. 
```
Здесь все как в предыдущем примере с `thenApplyAsync`, но параметр-функция уже от двух аргуметов,
и добавлен параметр `CompletionStage<? extends U> other`,
являющийся асинхронным поставщиком второго аргумента.
Как же нам обеспечить обработку второго аргумента? 
Ну во первых, вместо одной переменной `T arg` описать две: `T arg1;  U arg2;`,
a вместо одного метода public `void accept(T argument, Throwable throwable)` описать два - `accept1` и `accept2`,
каждый из которых работает со своим аргументом.
При этом наш строящийся объект уже не имплементирует интерфейс `BiConsumer<T,Throwable>` и мы не можем уже написать
ключевое предложение для связи узлов графа асинхронных вычислений
```java
        this.whenComplete(task);
```
По видимому, нам придется заключить код для приема аргумента в отдельный класс, и создать для каждого параметра свой экземпляр
этого класса. Придется, да, но пока что можно общйтись без этого.
К счастью, объект функционального интерфейса может быть изображен ссылкой на метод, без заключения его в отдельный класс:

```java
        this.whenComplete(task::accept1);
        other.whenComplete(task::accept2);
```
То есть, текущий объект поставляет первый аргумент, а объект `other` - второй.
Вот только коды методов придется изменить, чтобы он не запускали задачу при приходе своего аргумента, но также проверяли поступление и второго:

```java
        public synchronized void accept1(T argument, Throwable throwable) {
            if (throwable != null) {
                this.throwable = throwable;
                executor.execute(this);
            } else {
                this.arg1 = argument;
                if (arg2 != null) {
                    executor.execute(this);
                }
            }
        }
```
Аналогично описывается метод accept2.

Отметим, что:
- методы стали синхронизованными (работаем с общими данными)
- в случае передачи ошибки ждать второго аргумента не нужно.
- проверка поступления аргумента сравнением на **`null`** - не самый лучший способ, может, надо завести на каждый аргумент булевскую переменную.

Таким способом можно сделать асинхронные процедуры и от большего числа аргументов, чем два, но сразу приходит мысль - может все же сделать отдельный класс для
параметров, чтобы не писать для приема каждого параметра свой метод, а обходится динамическим созданием параметров?
```java
    Parameter<Integer> arg1 = new Parameter<>();
    Parameter<Float> arg2 = new Parameter<>();
    ...
    
    future1.whenComplete(arg1);
    future2.whenComplete(arg2);
```
Да, такой класс создать можно, но об этом в следующий раз.

Краткое резюме из вышеизложенного:

- асинхронная программа - это  сеть связанных между собой асинхронных процедур,
точно также, как мультипоточная программа - сеть связанных между собой потоков исполнения (threds).
Но средства связи потоков и асинхронных процедур кардинально отличаются.
Потоки связываются с помощью семафоров, блокирующих очередей и прочих подобных объектов,
которые блокируют поток получателя, если информация еще не поступила, но поток уже пытается ее извлечь с помощью pull-based операции.
Асинхронные процедуры - получатели просто не заходят на исполнение, пока на будет готова вся необходимая им информация.
Они пассивно ждут, пока поставщики информации сами не передадут ее с помощью push-based операция. 
Благодаря этому они не тратят память на стек во время ожидания, и, следовательно, занимают гораздо меньше памяти, чем потоки исполнения.

- построение сети асинхронных программ сводится к созданию объектов и связыванию их между собой.
Набор методов `CompletableFuture` ровно это и делает, и в принципе, без них можно обойтись, создавая объекты явно, как показано в приведенных выше примерах.
Но для этого надо иметь классы, аналогичные тем, что были описаны в этих примерах.
По какой-то причине создатели `java.util.concurrent` предпочли не давать пользователям доступ к этим классам и скрыли их в глубине кода `CompletableFuture`.

 - те, кто хочет иметь перед глазами наглядное представление создаваемой асинхронной сети, могут реконструировать эти классы, продолжив приведенные примеры.
Те, кто не хочет реконструировать сам, может воспользоваться готовой реконструкцией - [df4j](https://github.com/akaigoro/df4j).
Кроме реализации интерфейса [java.util.concurrent.CompletionStage](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html),
эта библиотека содержит потоки данных - простые и реактивные, акторы, а также экзотический асинхронный семафор.  

Вообще параллелей между многопоточным и асинхронным программированием очень много.
Можно взять любой способ обмена данными между потоками и построить для него асинхронный аналог. 
Mы только что рассмотрели `CompletablePromise` - асинхронный аналог `CompletableFuture`.
Для `java.util.concurrent.Semaphore` аналогом является [Semafor](https://github.com/akaigoro/df4j/blob/API-5/df4j-core/src/main/java/org/df4j/core/connector/permitstream/Semafor.java),
для `java.util.concurrent.BlockingQueue` -  [PickPoint](https://github.com/akaigoro/df4j/blob/API-5/df4j-core/src/main/java/org/df4j/core/node/messagestream/PickPoint.java).

Важно отметить, что как один поток может по очереди использовать разные методы обмена данными, так и одна асинхронная процедура может использовать разные методы,
но только не по очереди, как поток, а одновременно, заявив в своем описании коннекторы разных типов. 
При этом соединятся могут только выходные коннекторы одного типа со входными коннекторами того же типа.
Ноды тоже можно указывать при операциях соединения, но только если эти ноды реализуют интерфейс коннектора.
Обычно они это делают, перенаправляя вызовы коннекторного интерфейса внутреннему коннектору.
