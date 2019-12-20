package main.java.org.df4j.fancy.invoker;

public interface Invoker {

    Object apply(Object... args) throws Exception;

    default boolean returnsValue() {
        return false;
    }
}
