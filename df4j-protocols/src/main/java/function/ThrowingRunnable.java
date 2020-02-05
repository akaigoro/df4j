package function;

@FunctionalInterface
public interface ThrowingRunnable {
    void run() throws Throwable;
}
