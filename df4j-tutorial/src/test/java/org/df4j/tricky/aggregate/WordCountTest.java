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

import org.df4j.core.util.Pair;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public final class WordCountTest {
  private static final Path path = Paths.get("./src/test/resources/input.txt");

  @Test
  public void test1() throws Exception {
    ReduceByKey<String, Integer> reduce = new ReduceByKey<>((x, y) -> (int) x + (int) y);
    reduce.reduceByKey(new Pair<>("word", 1));
    for (ReduceByKey<String, Integer>.ReducingActor a: reduce.actors.values()) {
      a.onComplete();
      Pair<String, Integer> x = a.get(100, TimeUnit.SECONDS);
      System.out.println(x);
    }
  }

  @Test
  public void mainTest() throws Exception {
    ReduceByKey<String, Integer> reduce = new ReduceByKey<>((x, y) -> (int) x + (int) y);
    int wordcount=0;
    try (Stream<String> stream = Files.lines(path)) {
      stream.map(line -> line.split(" "))
              .flatMap(Arrays::stream)
              .forEach((word) -> reduce.reduceByKey(new Pair<>(word, 1)));
    }
//    reduce.onComplete(); // complete all actors
    for (ReduceByKey<String, Integer>.ReducingActor a: reduce.actors.values()) {
      a.onComplete();
      Pair<String, Integer> x = a.get(1, TimeUnit.SECONDS);
      System.out.println(x);
      wordcount++;
    }
    System.out.println("wordcount="+wordcount);
  }
}
