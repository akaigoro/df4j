package org.df4j.rxjava.protocol;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.functions.Function;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.util.concurrent.Executor;

public  class MapTest {

    @Test
    public void ClientTest_1() throws IOException, InterruptedException {
        Observable<String> just = Observable.just("Hello, world!");
        Function<String, Integer> stringIntegerFunction = s -> s.hashCode();
        Observable<Integer> map = just.map(stringIntegerFunction);
        Function<Integer, String> integerStringFunction = i -> Integer.toString(i);
        Observable<String> map1 = map.map(integerStringFunction);
        Consumer<String> stringConsumer = s -> System.out.println(s);
        map1.subscribe(stringConsumer);
    }
}