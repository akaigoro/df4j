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

import java.util.List;
import java.util.Map;
import org.df4j.pipeline.codec.javon.builder.JavonBulderFactory;
import org.df4j.pipeline.codec.javon.builder.ObjectBuilder;
import org.df4j.pipeline.codec.json.builder.*;
import org.df4j.pipeline.codec.json.builder.impl.JsonList;
import org.df4j.pipeline.codec.json.builder.impl.JsonMap;
import org.df4j.pipeline.codec.scanner.ParseException;

public class JavonBuilder extends ClassMap implements JavonBulderFactory {

    @Override
    public ObjectBuilder newObjectBuilder(String className) throws Exception {
        return new ObjectBuilderImpl(super.get(className));
    }

    @Override
    public ListBuilder newListBuilder() {
        return new ListBuilderImpl();
    }

    @Override
    public MapBuilder newMapBuilder() {
        return new MapBuilderImpl();
    }
    
    class ObjectBuilderImpl implements ObjectBuilder {
        ClassDescr descr;
        Object data;

        public ObjectBuilderImpl(ClassDescr descr) {
        	if (descr==null) {
        		throw new IllegalArgumentException("ClassDescr may not be null");
        	}
            this.descr=descr;
        }

        @Override
        public boolean isInstantiated() {
            return data!=null;
        }
        
        @Override
        public void instatntiate() throws Exception {
            data=descr.newInstance();
        }

        @Override
        public void instatntiate(Object... args) throws Exception {
            data=descr.newInstance(args);
        }

        @Override
        public void set(String key, Object value) throws Exception {
            descr.set(data, key, value);
        }

        @Override
        public ListBuilder asListBuilder() throws Exception {
            if (! (data instanceof List)) {
                throw new ParseException("class "+data.getClass()+" does not implement List");
            }
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) data;
            return new ListBuilderImpl(list);
        }

        @Override
        public MapBuilder asMapBuilder() throws ParseException {
            if (! (data instanceof Map)) {
                throw new ParseException("class "+data.getClass()+" does not implement Map");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) data;
            return new MapBuilderImpl(map);
        }

        @Override
        public Object getValue() {
            return data;
        }
    }
    
    class ListBuilderImpl implements ListBuilder {
        List<Object> data;
        
        public ListBuilderImpl(List<Object> list) {
            this.data=list;
        }

        public ListBuilderImpl() {
            this(new JsonList());
        }

        @Override
        public void add(Object value) {
            data.add(value);
        }

        @Override
        public Object getValue() {
            return data;
        }
    }
    
    class MapBuilderImpl implements MapBuilder {
        Map<String, Object> map;
        
        public MapBuilderImpl(Map<String, Object> map) {
            this.map=map;
        }

        public MapBuilderImpl() {
            this(new JsonMap());
        }

        @Override
        public void set(String key, Object value) {
            map.put(key, value);
        }

        @Override
        public Object getValue() {
            return map;
        }
    }
}
