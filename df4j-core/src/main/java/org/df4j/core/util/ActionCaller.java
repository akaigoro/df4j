package org.df4j.core.util;

import org.df4j.core.node.Action;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class ActionCaller<R> {
    private final static Class actionAnnotation = Action.class;
    private final Object actionObject;
    private final Method actionMethod;

    public ActionCaller(Object objectWithAction, int argCount) throws NoSuchMethodException {
        Class<?> startClass = objectWithAction.getClass();
        Object resultObject = null;
        Method resultMethod = null;
        for (Class<?> clazz = startClass; !Object.class.equals(clazz) ;clazz = clazz.getSuperclass()) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field: fields) {
                if (!field.isAnnotationPresent(actionAnnotation)) continue;
                field.setAccessible(true);
                Object lambda;
                try {
                    lambda = field.get(objectWithAction);
                } catch (IllegalAccessException e) {
                    continue;
                }
                if (lambda == null) continue;
                resultObject = lambda;
                //     Object v = field.get(this);
                //&& (field.getParameterTypes().length == argCount)
                for (Class<?> fieldType = field.getType(); fieldType != null && !Object.class.equals(fieldType) ;fieldType = fieldType.getSuperclass()) {
                    Method[] methods = fieldType.getDeclaredMethods();
                    for (Method m: methods) {
                        int modifiers = m.getModifiers();
                        if(!Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers) && !m.isDefault()) {
                            if (resultMethod != null) {
                                throw new NoSuchMethodException("class "+fieldType.getName()+" has more than one method; cannot be type of a field annotated with @Action.");
                            }
                            resultMethod=m;
                        }
                    }
                }
            }
            if (resultMethod != null) {
                if (resultMethod.getParameterTypes().length != argCount) {
                    throw new NoSuchMethodException("class "+startClass.getName()+" has a field annotated with @Action but with wrong numbers of parameters");
                }
                break;
            }
            Method[] methods = clazz.getDeclaredMethods();
            for (Method m: methods) {
                if (m.isAnnotationPresent(actionAnnotation)) {
                    if (resultMethod != null) {
                        throw new NoSuchMethodException("in class "+startClass.getName()+" more than one method annotated with @Action");
                    }
                    resultMethod = m;
                }
            }
            if (resultMethod != null) {
                if (resultMethod.getParameterTypes().length != argCount) {
                    throw new NoSuchMethodException("class "+startClass.getName()+" has a method annotated with @Action but with wrong numbers of parameters");
                }
                resultObject = objectWithAction;
                break;
            }
        }
        if (resultMethod == null) {
            throw new NoSuchMethodException("class "+startClass.getName()+" has no field or method annotated with @Action");
        }
        actionObject = resultObject;
        resultMethod.setAccessible(true);
        actionMethod = resultMethod;
    }

    public R apply(Object... args) throws Exception {
        Object res = actionMethod.invoke(actionObject, args);
        return (R) res;
    }

}
