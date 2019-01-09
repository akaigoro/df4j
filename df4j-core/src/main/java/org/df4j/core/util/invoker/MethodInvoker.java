package org.df4j.core.util.invoker;

import java.lang.reflect.Method;

public class MethodInvoker<R> implements Invoker {
    private final Object actionObject;
    private final Method actionMethod;
    private final boolean returnsValue;

    public MethodInvoker(Object actionObject, Method actionMethod) {
        this.actionObject = actionObject;
        this.actionMethod = actionMethod;
        Class<?> rt = actionMethod.getReturnType();
        returnsValue = !rt.equals(void.class);
    }

    public R apply(Object... args) throws Exception {
        Object res = actionMethod.invoke(actionObject, args);
        return (R) res;
    }

    @Override
    public boolean returnsValue() {
        return returnsValue;
    }
}
