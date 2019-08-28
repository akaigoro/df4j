package org.df4j.core.asyncproc;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static org.df4j.core.asyncproc.Promise.supplyAsync;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * made from CompletableFutureTest https://gist.github.com/spullara/5897605
 */
public class ScalarResultTest {


    @BeforeClass
    public static void setup() {
    }

    @Test
    public void testExceptions() {

        ScalarResult<Object> future = new ScalarResult<>();
        future.onError(new RuntimeException());
        future.exceptionally(t -> {
            t.printStackTrace();
            throw new CompletionException(t);
        });

        ScalarResult<Object> future2 = supplyAsync(() -> {
            throw new RuntimeException();
        });
        future2.exceptionally(t -> {
            t.printStackTrace();
            throw new CompletionException(t);
        });

        ScalarResult<String> future3 = supplyAsync(() -> "test");
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
        ScalarResult<String> other = supplyAsync(() -> "Doomed value");
        ScalarResult<String> future = supplyAsync(() -> {
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
    public void testCompleteExceptionally() throws ExecutionException, InterruptedException {
        AtomicBoolean cancelled = new AtomicBoolean();
        AtomicBoolean handled = new AtomicBoolean();
        AtomicBoolean handleCalledWithValue = new AtomicBoolean();
        ScalarResult<String> other = supplyAsync(() -> "Doomed value");
        ScalarResult<String> future = supplyAsync(() -> {
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
        future.onError(new CancellationException());
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
        ScalarResult<Object> future = new ScalarResult<>().exceptionally(t -> {
            called.set(true);
            return null;
        });
        future.onError(new CancellationException());
        try {
            future.get();
        } catch (Exception e) {
            System.out.println("exceptionally called: " + called);
        }
    }

    @Test
    public void testCompletableFutures() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        ScalarResult<String> future = supplyAsync(() -> {
            sleep(1000);
            return "Done.";
        });
        ScalarResult<String> future1 = supplyAsync(() -> {
            sleep(900);
            return "Done2.";
        });
        ScalarResult<String> future2 = supplyAsync(() -> "Constant");
        ScalarResult<String> future3 = supplyAsync(() -> {
            sleep(500);
            throw new RuntimeException("CompletableFuture4");
        });
        ScalarResult<String> future4 = new ScalarResult<>();
        future4.onError(new RuntimeException("CompletableFuture5"));
        ScalarResult<String> future5 = supplyAsync(() -> {
            executed.set(true);
            sleep(1000);
            return "Done.";
        });
        future5.cancel(true);

        ScalarResult<String> selected = select(future, future1, future3, future4);

        try {
            Assert.assertTrue(future5.isCancelled());
            Assert.assertTrue(future5.isDone());
            future5.get();
            fail("Was not cancelled");
        } catch (CancellationException ce) {
            if (executed.get()) {
                fail("Executed though cancelled immediately");
            }
        }

        ScalarResult<String> result10 = new ScalarResult<>();
        try {
            onFailure(future3, e -> {
                result10.onSuccess("Failed");
            }).get(0, TimeUnit.SECONDS);
            fail("Didn't timeout");
        } catch (TimeoutException te) {
        }

        try {
            future4.thenApply(v -> null).get();
            fail("Didn't fail");
        } catch (ExecutionException ee) {
        }

        ScalarResult<String> result3 = new ScalarResult<>();
        future.applyToEither(future1, v -> {result3.onSuccess("Selected: " + v); return null;});
        final ScalarResult<String> result4 = new ScalarResult<>();
        future1.applyToEither(future, v -> {result4.onSuccess("Selected: " + v); return null;});
        assertEquals("Selected: Done2.", result3.get());
        assertEquals("Selected: Done2.", result4.get());
        assertEquals("Done2.", selected.get());

        BiFunction<String, String, String> stringStringObjectBiFunction = (value1, value2) -> value1 + ", " + value2;
        ScalarResult<String> map1 = future.thenCombine(future1, stringStringObjectBiFunction);
        ScalarResult<String> map2 = future1.thenCombine(future, (value1, value2) -> value1 + ", " + value2);
        assertEquals("Done., Done2.", map1.get());
        assertEquals("Done2., Done.", map2.get());

        final ScalarResult<String> result1 = new ScalarResult<>();
        future.acceptEither(future3, s -> result1.onSuccess("Selected: " + s));
        assertEquals("Selected: Done.", result1.get());
        assertEquals("Failed", result10.get());

        try {
            onFailure(future3.acceptEither(future4, e -> {
            }), e -> {
                result1.onSuccess(e.getMessage());
            }).get();
            fail("Didn't fail");
        } catch (ExecutionException ee) {
        }

//        final CountDownLatch monitor = new CountDownLatch(2);
//        ScalarResult<String> onraise = supplyAsync(() -> {
//            try {
//                monitor.await();
//            } catch (InterruptedException e) {
//            }
//            return "Interrupted";
//        });
//        ScalarResult<String> join = future2.thenCombine(onraise, (a, b) -> null);
//        onraise.onRaise(e -> monitor.countDown());
//        onraise.onRaise(e -> monitor.countDown());

//        ScalarResult<String> map = future.map(v -> "Set1: " + v).map(v -> {
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

        ScalarResult<String> result11 = new ScalarResult<>();
        try {
            onFailure(future1.thenApply(v -> future3), e -> {
                result11.onSuccess("Failed");
            }).get();
        } catch (ExecutionException ee) {
            assertEquals("Failed", result11.get());
        }

        ScalarResult<String> result2 = new ScalarResult<>();
        onFailure(future3.thenCompose(v -> future1), e -> {
            result2.onSuccess("Flat map failed: " + e);
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

        ScalarResult<String> result5 = new ScalarResult<>();
        ScalarResult<String> result6 = new ScalarResult<>();
        onFailure(future.thenAccept(s -> result5.onSuccess("onSuccess: " + s)),
                e -> result5.onSuccess("onFailure: " + e))
                .thenRun(() -> result6.onSuccess("Ensured"));
        assertEquals("onSuccess: Done.", result5.get());
        assertEquals("Ensured", result6.get());

        ScalarResult<String> result7 = new ScalarResult<>();
        ScalarResult<String> result8 = new ScalarResult<>();
        ensure(onFailure(future3.thenAccept(s -> result7.onSuccess("onSuccess: " + s)), e -> {
            result7.onSuccess("onFailure: " + e);
        }), () -> result8.onSuccess("Ensured"));
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

        ScalarResult<String> result9 = new ScalarResult<>();
        future.thenAccept(v -> result9.onSuccess("onSuccess: " + v));
        assertEquals("onSuccess: Done.", result9.get());
    }

    private ScalarResult<List<String>> collect(List<ScalarResult<String>> completableFutures) {
        ScalarResult<List<String>> result = new ScalarResult<>();
        int size = completableFutures.size();
        List<String> list = new ArrayList<>();
        if (size == 0) {
            result.onSuccess(list);
        } else {
            for (ScalarResult<String> completableFuture : completableFutures) {
                completableFuture.handle((s, t) -> {
                    if (t == null) {
                        list.add(s);
                        if (list.size() == size) {
                            result.onSuccess(list);
                        }
                    } else {
                        result.onError(t);
                    }
                    return s;
                });
            }
        }
        return result;
    }

    private static <T> ScalarResult<T> onFailure(Promise<T> future, Consumer<Throwable> call) {
        ScalarResult<T> completableFuture = new ScalarResult<>();
        future.handle((v, t) -> {
            if (t == null) {
                completableFuture.onSuccess(v);
            } else {
                call.accept(t);
                completableFuture.onError(t);
            }
            return null;
        });
        return completableFuture;
    }

    private static <T> ScalarResult<T> ensure(ScalarResult<T> future, Runnable call) {
        ScalarResult<T> completableFuture = new ScalarResult<>();
        future.handle((v, t) -> {
            if (t == null) {
                call.run();
                completableFuture.onSuccess(v);
            } else {
                call.run();
                completableFuture.onError(t);
            }
            return null;
        });
        return completableFuture;
    }

    @SafeVarargs
    private final ScalarResult<String> select(ScalarResult<String>... completableFutures) {
        ScalarResult<String> future = new ScalarResult<>();
        for (ScalarResult<String> completableFuture : completableFutures) {
            completableFuture.thenAccept(t -> future.onSuccess(t));
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