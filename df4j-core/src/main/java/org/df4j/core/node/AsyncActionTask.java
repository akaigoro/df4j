package org.df4j.core.node;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AsyncActionTask<R> extends AsyncResult<R> {
    private Method actionMethod;

    static private Method findActionMethod(Class<?> startClass, int argCount) throws NoSuchMethodException {
        Class actionAnnotation = Action.class;
        Method result = null;
        for (Class<?> clazz = startClass; !Object.class.equals(clazz) ;clazz = clazz.getSuperclass()) {
            Method[] methods = clazz.getDeclaredMethods();
            for (Method m: methods) {
                if (m.isAnnotationPresent(actionAnnotation) && (m.getParameterTypes().length == argCount)) {
                    if (result != null) {
                        throw new NoSuchMethodException("in class "+startClass.getName()+" more than one method annotated as Action with "+argCount+" paramenters found");
                    }
                    result = m;
                }
            }
            if (result != null) {
                result.setAccessible(true);
                return result;
            }
        }
        throw new NoSuchMethodException("in class "+startClass.getName()+" no one method annotated as Action with "+argCount+" paramenters found");
    }

    protected void runAction() throws IllegalAccessException, InvocationTargetException {
        controlLock.turnOff();
        if (actionMethod == null) {
            try {
                actionMethod = findActionMethod(getClass(), connectors.size());
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
        }
        Object[] args = consumeTokens();
        actionMethod.invoke(this, args);
    }

    @Override
    public void run() {
        try {
            runAction();
        } catch (Throwable e) {
            stop();
            // TODO move to failed state
            System.err.println("Error in async task " + getClass().getName());
            e.printStackTrace();
        }
    }
}
