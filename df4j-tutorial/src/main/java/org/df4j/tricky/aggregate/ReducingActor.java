package org.df4j.tricky.aggregate;

import org.df4j.core.actor.ClassicActor;
import org.df4j.core.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;

class ReducingActor<K, V> extends ClassicActor<Pair<K, V>> {
    private final K key;
    private final BiFunction<V, V, V> reducer;
    private V state;
    private Pair<K, V> result;

    ReducingActor(K key, BiFunction<V, V, V> reducer) {
        this.key = key;
        this.reducer = reducer;
        start();
    }

    @Override
    protected void runAction(Pair<K, V> msg) {
        state = msg.getValue();
        nextMessageAction(this::reduce);
    }

    public void reduce(Pair<K, V> msg) {
        state = reducer.apply(state, msg.getValue());
    }

    @Override
    public synchronized void whenComplete(Throwable ex) {
        this.result = new Pair<>(key, state);
    }

    public Pair<K, V> get(long timeout, @NotNull TimeUnit unit) throws TimeoutException, InterruptedException {
        boolean ok = await(timeout, unit);
        if (!ok) {
            throw new TimeoutException();
        }
        return result;
    }
}
