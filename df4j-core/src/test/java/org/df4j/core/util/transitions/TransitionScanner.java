package org.df4j.core.util.transitions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

public class TransitionScanner {
    private final static Class<Transitions> transitionsAnnotation = Transitions.class;
    private final static Class<Transition> transitionAnnotation = Transition.class;

    public static void findTransitions( Class<?> startClass) {
        HashMap<String, ArrayList<Field>> transitions = new HashMap<>();
        classScan:
        for (Class<?> clazz = startClass; !Object.class.equals(clazz) ;clazz = clazz.getSuperclass()) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field: fields) {
                field.setAccessible(true);
                Transitions tsa = field.getAnnotation(transitionsAnnotation);//(transitionsAnnotation);
                String[] tss = tsa.transitions();
                Transitions t = tsa;
                
            }
            Method[] methods = clazz.getDeclaredMethods();
            for (Method m: methods) {
                Annotation ta = m.getDeclaredAnnotation(transitionAnnotation);
                Annotation t = ta;

            }
        }
    }

    static class TransitionSet {
        String name;
        Field[] ports;
    }
}
