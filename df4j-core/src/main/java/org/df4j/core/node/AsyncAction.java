/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.core.node;

import org.df4j.core.node.messagescalar.AsyncResult;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/**
 * AsyncTask is a reusable Asynchronous Procedure Call: after execution, it executes again as soon as new array of arguments is ready.
 *
 * It consists of asynchronous connectors, implemented as inner classes,
 * user-defined asynchronous procedure, and a asyncTask mechanism to call that procedure
 * using supplied {@link Executor} as soon as all connectors are unblocked.
 *
 * This class contains connectors for following protocols:
 *  - scalar messages
 *  - message stream (without back pressure)
 *  - permit stream
 */
public abstract class AsyncAction<R> extends AsyncResult<R> {

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

    public synchronized Object[] consumeTokens() {
        if (!isStarted()) {
            throw new IllegalStateException("not started");
        }
        locks.forEach(lock -> lock.purge());
        Object[] args = new Object[connectors.size()];
        for (int k=0; k<connectors.size(); k++) {
            Connector connector = connectors.get(k);
            args[k] = connector.next();
        }
        return args;
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