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

import java.util.ArrayList;
import java.util.Iterator;

/**
 * A List that prints itself in Json style
 */
public class JsonList extends ArrayList<Object>{

    @Override
    public String toString() {
        Iterator<Object> it = iterator();
        if (! it.hasNext())
            return "[]";
        
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (;;) {
            Object e = it.next();
            if (e == this) {
                sb.append("(this Collection)");
            } else if (e instanceof String){
                sb.append('"');
                sb.append(e);
                sb.append('"');
            } else {
                sb.append(e);
            }
            if (! it.hasNext())
                return sb.append(']').toString();
            sb.append(',');
        }
    }
    
}