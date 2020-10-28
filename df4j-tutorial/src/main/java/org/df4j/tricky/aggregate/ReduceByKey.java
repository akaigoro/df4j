package org.df4j.tricky.aggregate;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.df4j.core.actor.ClassicActor;
import org.df4j.core.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;

/**
 * a class to make reduce operation similar to Spark's RDD::reduceByKey
 * @param <K>
 */
public final class ReduceByKey<K, V> {
  final BiFunction<V,V,V> reducer;
  protected final ConcurrentHashMap<K,ReducingActor> actors = new ConcurrentHashMap<>();

  public ReduceByKey(BiFunction<V,V,V> reducer) {
    this.reducer = reducer;
  }

  public void reduceByKey(K key, V value) {
    Pair<K, V> msg = new Pair<>(key, value);
    ReducingActor actor = actors.computeIfAbsent(key,
            (key1) -> new ReducingActor(key1));
    actor.onNext(msg);
  }

  public void onComplete() {
    for (ReducingActor actor: actors.values()) {
      actor.onComplete();
    }
  }

  class ReducingActor extends ClassicActor<Pair<K, V>> {
    private V state;
    private Pair<K, V> result;
    private final K key;

    ReducingActor(K key) {
      this.key = key;
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
    public synchronized void whenComplete() {
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
}
