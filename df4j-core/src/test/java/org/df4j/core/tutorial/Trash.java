package org.df4j.core.tutorial;

import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

public class Trash {

    @Test
    public void test() throws ExecutionException, InterruptedException {
        long n=Long.MAX_VALUE;
        n+=Long.MAX_VALUE;
        System.out.println("n="+n);
        CompletableFuture cf = new CompletableFuture();
        BiConsumer cc = new BiConsumer() {
            @Override
            public void accept(Object o, Object o2) {
                if (o!=o2) {
                    return;
                }
            }
        };
        cf.whenComplete(cc);
  //      cf.complete(1);
        cf.complete(null);
        cf.completeExceptionally(new RuntimeException());
        Object res = cf.get();
    }
}
