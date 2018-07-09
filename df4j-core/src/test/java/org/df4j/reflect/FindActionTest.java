package org.df4j.reflect;

import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class FindActionTest {
interface I3 {
    double m3(String s, int n, float f);
}

static class F2  {
    public float m2(String s, int n){return n;}

}
    public double m2(String s, int n){return n;}
    public double m3(String s, int n, float f){return n;}
    Runnable r = ()->{};
    Consumer<String> c1 = System.out::println;
    BiFunction<String, Integer, Double> f2 = this::m2;
    I3 f3 =this::m3;
    F2 ff = new F2();

    void printType(Object... args) {
        for (Object arg: args) {
            printType1(arg);
        }
    }

    private void printType1(Object arg) {
        System.out.println("type="+arg.getClass());
        try {
            Method m = getMethodForFunctionalInterface(arg);
            System.out.println("method="+m);
        } catch (Exception e) {
            System.out.println("exception"+e);
        }
    }

    @Test
    public void lambdaType() {
        printType(r,c1,f2, f3, ff);
    }

    public Object callWhatever(final Object o, final Object... params) throws Exception
    {
        if (o instanceof Runnable) {
            ((Runnable)o).run();
            return null;
        }

        if (o instanceof Callable) {
            return ((Callable<?>)o).call();
        }

        if (o instanceof Method) {
            return ((Method)o).invoke(params[0], Arrays.copyOfRange(params, 1, params.length));
        }

        final Method method = getMethodForFunctionalInterface(o);
        if (method != null) {
            return method.invoke(o, params);
        }

        throw new InvalidParameterException("Object of type " + o.getClass() +  " is not callable!");
    }

    public Method getMethodForFunctionalInterface(final Object o) {
        Class<?> clazz = o.getClass();
        while (clazz != null) {
            for (final Class<?> interfaze : clazz.getInterfaces()) {
                for (final Method method : interfaze.getDeclaredMethods()) {
                    if (Modifier.isAbstract(method.getModifiers())) {
                        return method;
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }
}
