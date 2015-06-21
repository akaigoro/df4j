/*
 * Copyright 2013 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.pipeline.codec.javon.builder.impl;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.TreeSet;
import org.df4j.pipeline.codec.scanner.ParseException;

public class ClassDescr {
    private static final HashMap<Class<?>, Class<?>> PRIMITIVES_TO_WRAPPERS
    = new HashMap<Class<?>, Class<?>>();
    {
      PRIMITIVES_TO_WRAPPERS.put(boolean.class, Boolean.class);
      PRIMITIVES_TO_WRAPPERS.put(byte.class, Byte.class);
      PRIMITIVES_TO_WRAPPERS.put(char.class, Character.class);
      PRIMITIVES_TO_WRAPPERS.put(double.class, Double.class);
      PRIMITIVES_TO_WRAPPERS.put(float.class, Float.class);
      PRIMITIVES_TO_WRAPPERS.put(int.class, Integer.class);
      PRIMITIVES_TO_WRAPPERS.put(long.class, Long.class);
      PRIMITIVES_TO_WRAPPERS.put(short.class, Short.class);
      PRIMITIVES_TO_WRAPPERS.put(void.class, Void.class);
    }
    
    Class<?> clazz;
    BeanInfo bi;
    private HashMap<String, PropertyDescriptor> props = new HashMap<String, PropertyDescriptor>();
    String[] sequence=null;
    boolean ignoreCase=false;
    TreeSet<String> seqSet=new TreeSet<String>();

    ClassDescr(Class<?> clazz) throws IntrospectionException {
        this.clazz=clazz;
        BeanInfo bi = Introspector.getBeanInfo(clazz);
        PropertyDescriptor[] pds=bi.getPropertyDescriptors();
        for (PropertyDescriptor pd: pds) {
            props.put(pd.getName(), pd);
        }
//        ignoreCase=(clazz.getAnnotation(IgnoreCase.class)!=null);
        /*
        Sequence seq = clazz.getAnnotation(Sequence.class);
        if (seq != null) {
            sequence = seq.value();
            for (int k=0; k<sequence.length; k++) {
                String attrName=sequence[k];
                if (ignoreCase) {
                    attrName = attrName.toLowerCase();
                }
                sequence[k]=attrName;
                seqSet.add(attrName);
            }
        }
        */
        if (sequence == null) {
            sequence = seqSet.toArray(new String[seqSet.size()]);
        } else {
            for (String attr: sequence) {
                if (props.get(attr)==null) {
                    throw new IntrospectionException("invalid attr:"+clazz.getName()+"."+attr); 
                }
            }
        }
    }
 
    public ClassDescr(String className) throws ClassNotFoundException, IntrospectionException {
        this(Class.forName(className));
    }

    public Object newInstance() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        return clazz.newInstance();
    }
    
    public Object newInstance(Object... args) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (args.length==0) {
            return clazz.newInstance();
        }
        Constructor<?> bestCt=null;
        Constructor<?>[] cts=clazz.getConstructors();
        ctsLoop:
        for (Constructor<?> ct: cts) {
            Class<?>[] pts=ct.getParameterTypes();
            if (pts.length!=args.length) {
                continue;
            }
            boolean perfectMatch=true;
            for (int k=0; k<pts.length; k++) {
                Class<?> pt=pts[k];
                Object arg=args[k];
                if (arg==null) {
                    if (pt.isPrimitive()) {
                        continue ctsLoop; // never match
                    } else {
                        continue; // always match
                    }
                }
                Class<?> at=arg.getClass();
                if (at==pt) {
                    continue;
                }
                if (pt.isPrimitive()) {
                    if (PRIMITIVES_TO_WRAPPERS.get(pt)==at) {
                        continue;
                    } else {
                        continue ctsLoop;
                    }
                }
                perfectMatch=false;
                if (!pt.isAssignableFrom(at)) {
                    continue ctsLoop;
                }
            }
            bestCt=ct;
            if (perfectMatch) {
                break;
            }
        }
        if (bestCt==null) {
            throw new InstantiationException();
        }
        Object res=bestCt.newInstance(args);
        return res;
    }
    
    public Object get(Object data, String attrName) throws ParseException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (ignoreCase) {
            attrName=attrName.toLowerCase();
        }
        PropertyDescriptor prop = props.get(attrName);
        if (prop == null) {
            throw new ParseException("No such attribute:" + attrName);
        }
        return prop.getReadMethod().invoke(data);
    }

    public void set(Object data, String attrName, Object value) throws ParseException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (ignoreCase) {
            attrName=attrName.toLowerCase();
        }
        PropertyDescriptor prop = props.get(attrName);
        if (prop == null) {
            throw new ParseException("No such attribute:" + attrName);
        }
        prop.getWriteMethod().invoke(data, value);
    }

    public Class<?> getClazz() {
        return clazz;
    }
}

