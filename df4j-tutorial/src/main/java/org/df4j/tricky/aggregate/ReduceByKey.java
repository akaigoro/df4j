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

import javafx.util.Pair;
import org.df4j.core.communicator.ScalarResultTrait;
import org.df4j.core.dataflow.ClassicActor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * a class to make reduce operation similar to Spark's RDD::reduceByKey
 * @param <K>
 */
public final class ReduceByKey<K, V> {
  private final BiFunction<V,V,V> reducer;
  protected final ConcurrentHashMap<K,ReducingActor> actors = new ConcurrentHashMap<>();

  public ReduceByKey(BiFunction<V,V,V> reducer) {
    this.reducer = reducer;
  }

  public void reduceByKey(Pair<K,V> msg) {
    ReducingActor actor = actors.computeIfAbsent(msg.getKey(), (key) -> {ReducingActor res = new ReducingActor(key); res.start(); return res;});
    actor.onNext(msg);
  }

  public void onComplete() {
    for (ReducingActor actor: actors.values()) {
      actor.onComplete();
    }
  }

  class ReducingActor extends ClassicActor<Pair<K,V>> implements ScalarResultTrait<Pair<K,V>> {
    private V state;
    private Pair<K,V> result;
    private final K key;

    ReducingActor(K key) {
      this.key = key;
    }

    @Override
    public void setResult(Pair<K, V> result) {
      this.result = result;
    }

    @Override
    public Pair<K, V> getResult() {
      return result;
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
    public void complete() {
      super.complete();
      setResult(new Pair<>(key, state));
    }
  }
}
