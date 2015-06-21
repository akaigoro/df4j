/*
 * Copyright 2013 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.pipeline.codec.json.builder.impl;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

/**
 * A Map that prints itself in Json style
 */
public class JsonMap extends LinkedHashMap<String,Object> {

    @Override
    public String toString() {
        Iterator<Entry<String,Object>> i = entrySet().iterator();
        if (! i.hasNext())
            return "{}";
        
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        for (;;) {
            Entry<String,Object> e = i.next();
            String key = e.getKey();
            Object value = e.getValue();
            sb.append('"');
            sb.append(key);
            sb.append('"');
            sb.append(':');
            if (value instanceof String){
                sb.append('"');
                sb.append(value);
                sb.append('"');
            } else {
                sb.append(value);
            }
            if (! i.hasNext())
                return sb.append('}').toString();
            sb.append(',');
        }
    }
    
}