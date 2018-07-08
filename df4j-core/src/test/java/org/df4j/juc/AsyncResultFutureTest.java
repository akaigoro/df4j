package org.df4j.juc;

import org.df4j.core.node.messagescalar.AsyncResult;
import org.df4j.core.node.messagescalar.AsyncResultFuture;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.df4j.core.node.messagescalar.AsyncResultFuture.supplyAsync;

/**
 * made from CompletableFutureTest https://gist.github.com/spullara/5897605
 */
public class AsyncResultFutureTest {

    public static void main(String[] args) throws Exception {
        AsyncResultFutureTest pt = new AsyncResultFutureTest();
        setup();
        pt.testCompletableFutures();
        System.out.println("Success.");
    }

    @BeforeClass
    public static void setup() {
    }

    @Test
    public void testExceptions() {

        AsyncResultFuture<Object> future = new AsyncResultFuture<>();
        future.completeExceptionally(new RuntimeException());
        future.exceptionally(t -> {
            t.printStackTrace();
            throw new CompletionException(t);
        });

        AsyncResultFuture<Object> future2 = supplyAsync(() -> {
            throw new RuntimeException();
        });
        future2.exceptionally(t -> {
            t.printStackTrace();
            throw new CompletionException(t);
        });

        AsyncResultFuture<String> future3 = supplyAsync(() -> "test");
        future3.thenAccept(t -> {
            throw new RuntimeException();
        }).exceptionally(t -> {
            t.printStackTrace();
            throw new CompletionException(t);
        });
    }

    @Test
    public void testCancellation() throws ExecutionException, InterruptedException {
        AtomicBoolean cancelled = new AtomicBoolean();
        AtomicBoolean handled = new AtomicBoolean();
        AtomicBoolean handleCalledWithValue = new AtomicBoolean();
        AsyncResultFuture<String> other = supplyAsync(() -> "Doomed value");
        AsyncResultFuture<String> future = supplyAsync(() -> {
            sleep(1000);
            return "Doomed value";
        }).exceptionally(t -> {
            cancelled.set(true);
            return null;
        }).thenCombine(other, (a, b) -> a + ", " + b).handle((v, t) -> {
            if (t == null) {
                handleCalledWithValue.set(true);
            }
            handled.set(true);
            return null;
        });
        sleep(100);
        future.cancel(true);
        sleep(1000);
        try {
            future.get(1, TimeUnit.SECONDS);
            fail("Should have thrown");
        } catch (CancellationException ce) {
            System.out.println("future cancelled: " + future.isCancelled());
            System.out.println("other cancelled: " + other.isCancelled());
            System.out.println("exceptionally called: " + cancelled.get());
            System.out.println("handle called: " + handled.get());
            System.out.println("handle called with value: " + handleCalledWithValue.get());
        } catch (TimeoutException e) {
            fail("Should have thrown");
        }
    }

    @Test
    public void testCompleteExceptionally() throws ExecutionException, InterruptedException {
        AtomicBoolean cancelled = new AtomicBoolean();
        AtomicBoolean handled = new AtomicBoolean();
        AtomicBoolean handleCalledWithValue = new AtomicBoolean();
        AsyncResultFuture<String> other = supplyAsync(() -> "Doomed value");
        AsyncResultFuture<String> future = supplyAsync(() -> {
            sleep(1000);
            return "Doomed value";
        }).exceptionally(t -> {
            cancelled.set(true);
            return null;
        }).thenCombine(other, (a, b) -> a + ", " + b).handle((v, t) -> {
            if (t == null) {
                handleCalledWithValue.set(true);
            }
            handled.set(true);
            return null;
        });
        sleep(100);
        future.completeExceptionally(new CancellationException());
        sleep(1000);
        try {
            future.get();
            fail("Should have thrown");
        } catch (CancellationException ce) {
            System.out.println("future cancelled: " + future.isCancelled());
            System.out.println("other cancelled: " + other.isCancelled());
            System.out.println("exceptionally called: " + cancelled.get());
            System.out.println("handle called: " + handled.get());
            System.out.println("handle called with value: " + handleCalledWithValue.get());
        }
    }

    @Test
    public void testExceptionally() {
        AtomicBoolean called = new AtomicBoolean();
        AsyncResultFuture<Object> future = new AsyncResultFuture<>().exceptionally(t -> {
            called.set(true);
            return null;
        });
        future.completeExceptionally(new CancellationException());
        try {
            future.get();
        } catch (Exception e) {
            System.out.println("exceptionally called: " + called);
        }
    }

    @Test
    public void testCompletableFutures() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        AsyncResultFuture<String> future = supplyAsync(() -> {
            sleep(1000);
            return "Done.";
        });
        AsyncResultFuture<String> future1 = supplyAsync(() -> {
            sleep(900);
            return "Done2.";
        });
        AsyncResultFuture<String> future2 = supplyAsync(() -> "Constant");
        AsyncResultFuture<String> future3 = supplyAsync(() -> {
            sleep(500);
            throw new RuntimeException("CompletableFuture4");
        });
        AsyncResultFuture<String> future4 = new AsyncResultFuture<>();
        future4.completeExceptionally(new RuntimeException("CompletableFuture5"));
        AsyncResultFuture<String> future5 = supplyAsync(() -> {
            executed.set(true);
            sleep(1000);
            return "Done.";
        });
        future5.cancel(true);

        AsyncResultFuture<String> selected = select(future, future1, future3, future4);

        try {
            junit.framework.Assert.assertTrue(future5.isCancelled());
            junit.framework.Assert.assertTrue(future5.isDone());
            future5.get();
            fail("Was not cancelled");
        } catch (CancellationException ce) {
            if (executed.get()) {
                fail("Executed though cancelled immediately");
            }
        }

        AsyncResultFuture<String> result10 = new AsyncResultFuture<>();
        try {
            onFailure(future3, e -> {
                result10.complete("Failed");
            }).get(0, TimeUnit.SECONDS);
            fail("Didn't timeout");
        } catch (TimeoutException te) {
        }

        try {
            future4.thenApply(v -> null).get();
            fail("Didn't fail");
        } catch (ExecutionException ee) {
        }

        AsyncResultFuture<String> result3 = new AsyncResultFuture<>();
        future.applyToEither(future1, v -> result3.complete("Selected: " + v));
        final AsyncResultFuture<String> result4 = new AsyncResultFuture<>();
        future1.applyToEither(future, v -> result4.complete("Selected: " + v));
        assertEquals("Selected: Done2.", result3.get());
        assertEquals("Selected: Done2.", result4.get());
        assertEquals("Done2.", selected.get());

        AsyncResultFuture<String> map1 = future.thenCombine(future1, (value1, value2) -> value1 + ", " + value2);
        AsyncResultFuture<String> map2 = future1.thenCombine(future, (value1, value2) -> value1 + ", " + value2);
        assertEquals("Done., Done2.", map1.get());
        assertEquals("Done2., Done.", map2.get());

        final AsyncResultFuture<String> result1 = new AsyncResultFuture<>();
        future.acceptEither(future3, s -> result1.complete("Selected: " + s));
        assertEquals("Selected: Done.", result1.get());
        assertEquals("Failed", result10.get());

        try {
            onFailure(future3.acceptEither(future4, e -> {
            }), e -> {
                result1.complete(e.getMessage());
            }).get();
            fail("Didn't fail");
        } catch (ExecutionException ee) {
        }

//        final CountDownLatch monitor = new CountDownLatch(2);
//        AsyncResultFuture<String> onraise = supplyAsync(() -> {
//            try {
//                monitor.await();
//            } catch (InterruptedException e) {
//            }
//            return "Interrupted";
//        });
//        AsyncResultFuture<String> join = future2.thenCombine(onraise, (a, b) -> null);
//        onraise.onRaise(e -> monitor.countDown());
//        onraise.onRaise(e -> monitor.countDown());

//        AsyncResultFuture<String> map = future.map(v -> "Set1: " + v).map(v -> {
//            join.raise(new CancellationException());
//            return "Set2: " + v;
//        });

//        assertEquals("Set2: Set1: Done.", map.get());
//        assertEquals(new Pair<>("Constant", "Interrupted"), join.get());
//
        try {
            future.thenCombine(future3, (value1, value2) -> value1 + ", " + value2).get();
            fail("Didn't fail");
        } catch (ExecutionException ee) {
        }

        assertEquals("Flatmapped: Constant", future1.thenCompose(v -> future2).thenApply(v -> "Flatmapped: " + v).get());

        AsyncResultFuture<String> result11 = new AsyncResultFuture<>();
        try {
            onFailure(future1.thenApply(v -> future3), e -> {
                result11.complete("Failed");
            }).get();
        } catch (ExecutionException ee) {
            assertEquals("Failed", result11.get());
        }

        AsyncResultFuture<String> result2 = new AsyncResultFuture<>();
        onFailure(future3.thenCompose(v -> future1), e -> {
            result2.complete("Flat map failed: " + e);
        });
        assertEquals("Flat map failed: java.util.concurrent.CompletionException: java.lang.RuntimeException: CompletableFuture4", result2.get());

        assertEquals("Done.", future.get(1, TimeUnit.DAYS));

        try {
            future3.get();
            fail("Didn't fail");
        } catch (ExecutionException e) {
        }

        try {
            future3.thenCombine(future, (a, b) -> null).get();
            fail("Didn't fail");
        } catch (ExecutionException e) {
        }

        AsyncResultFuture<String> result5 = new AsyncResultFuture<>();
        AsyncResultFuture<String> result6 = new AsyncResultFuture<>();
        onFailure(future.thenAccept(s -> result5.complete("onSuccess: " + s)),
                e -> result5.complete("onFailure: " + e))
                .thenRun(() -> result6.complete("Ensured"));
        assertEquals("onSuccess: Done.", result5.get());
        assertEquals("Ensured", result6.get());

        AsyncResultFuture<String> result7 = new AsyncResultFuture<>();
        AsyncResultFuture<String> result8 = new AsyncResultFuture<>();
        ensure(onFailure(future3.thenAccept(s -> result7.complete("onSuccess: " + s)), e -> {
            result7.complete("onFailure: " + e);
        }), () -> result8.complete("Ensured"));
        assertEquals("onFailure: java.util.concurrent.CompletionException: java.lang.RuntimeException: CompletableFuture4", result7.get());
        assertEquals("Ensured", result8.get());

        assertEquals("Was Rescued!", future3.exceptionally(e -> "Rescued!").thenApply(v -> "Was " + v).get());
        assertEquals("Was Constant", future2.exceptionally(e -> "Rescued!").thenApply(v -> "Was " + v).get());

        assertEquals(asList("Done.", "Done2.", "Constant"), collect(asList(future, future1, future2)).get());
        assertEquals(Arrays.<String>asList(), collect(new ArrayList<>()).get());
        try {
            assertEquals(asList("Done.", "Done2.", "Constant"), collect(asList(future, future3, future2)).get());
            fail("Didn't fail");
        } catch (ExecutionException ee) {
        }

        AsyncResultFuture<String> result9 = new AsyncResultFuture<>();
        future.thenAccept(v -> result9.complete("onSuccess: " + v));
        assertEquals("onSuccess: Done.", result9.get());
    }

    private AsyncResultFuture<List<String>> collect(List<AsyncResultFuture<String>> completableFutures) {
        AsyncResultFuture<List<String>> result = new AsyncResultFuture<>();
        int size = completableFutures.size();
        List<String> list = new ArrayList<>();
        if (size == 0) {
            result.complete(list);
        } else {
            for (AsyncResultFuture<String> completableFuture : completableFutures) {
                completableFuture.handle((s, t) -> {
                    if (t == null) {
                        list.add(s);
                        if (list.size() == size) {
                            result.complete(list);
                        }
                    } else {
                        result.completeExceptionally(t);
                    }
                    return s;
                });
            }
        }
        return result;
    }

    private static <T> AsyncResultFuture<T> onFailure(AsyncResultFuture<T> future, Consumer<Throwable> call) {
        AsyncResultFuture<T> completableFuture = new AsyncResultFuture<>();
        future.handle((v, t) -> {
            if (t == null) {
                completableFuture.complete(v);
            } else {
                call.accept(t);
                completableFuture.completeExceptionally(t);
            }
            return null;
        });
        return completableFuture;
    }

    private static <T> AsyncResultFuture<T> ensure(AsyncResultFuture<T> future, Runnable call) {
        AsyncResultFuture<T> completableFuture = new AsyncResultFuture<>();
        future.handle((v, t) -> {
            if (t == null) {
                call.run();
                completableFuture.complete(v);
            } else {
                call.run();
                completableFuture.completeExceptionally(t);
            }
            return null;
        });
        return completableFuture;
    }

    @SafeVarargs
    private final AsyncResultFuture<String> select(AsyncResultFuture<String>... completableFutures) {
        AsyncResultFuture<String> future = new AsyncResultFuture<>();
        for (AsyncResultFuture<String> completableFuture : completableFutures) {
            completableFuture.thenAccept(future::complete);
        }
        return future;
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

    void run(Runnable run) {
        run.run();
    }
}