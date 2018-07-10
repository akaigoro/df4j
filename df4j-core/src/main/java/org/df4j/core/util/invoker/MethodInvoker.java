package org.df4j.core.util.invoker;

import java.lang.reflect.Method;

public class MethodInvoker<R> implements Invoker<R> {
    private final Object actionObject;
    private final Method actionMethod;

    public MethodInvoker(Object actionObject, Method actionMethod) {
        this.actionObject = actionObject;
        this.actionMethod = actionMethod;
    }

    @Override
    public boolean isEmpty() {
        return actionMethod == null;
    }

    public R apply(Object... args) throws Exception {
        Object res = actionMethod.invoke(actionObject, args);
        return (R) res;
    }

}
