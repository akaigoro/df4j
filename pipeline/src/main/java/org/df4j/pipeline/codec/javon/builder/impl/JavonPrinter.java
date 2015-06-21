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

import org.df4j.pipeline.codec.javon.builder.JavonBulderFactory;
import org.df4j.pipeline.codec.javon.builder.ObjectBuilder;
import org.df4j.pipeline.codec.json.builder.ListBuilder;
import org.df4j.pipeline.codec.json.builder.MapBuilder;
import org.df4j.pipeline.codec.json.builder.impl.JsonPrinter;

public class JavonPrinter extends JsonPrinter implements JavonBulderFactory {
    Printer pr=new Printer();

    @Override
    public ObjectBuilder newObjectBuilder(String className) throws Exception {
        pr.log("newObjectBuilder", className);
        return new ObjectBuilderImpl(className);
    }

    class ObjectBuilderImpl extends Printer implements ObjectBuilder {
        String className;
        boolean first=true;
        boolean instantiated=false;

        public ObjectBuilderImpl(String className) {
            this.className=className;
        }

        @Override
        public boolean isInstantiated() {
            return instantiated;
        }

        @Override
        public void instatntiate() throws Exception {
            instantiated=true;
            log("instatntiate");
            sb.append(className);
        }

        @Override
        public void instatntiate(Object... args) throws Exception {
            instantiated=true;
            log("instatntiate", args);
            sb.append(className);
            if (args.length>0) {
                sb.append('(');
                setCloseChar(')');
                for (Object arg: args) {
                    if (first) {
                        first=false;
                    } else {
                        sb.append(",");
                    }
                    printValue(arg);
                }
            }
        }

        @Override
        public void set(String key, Object value) throws Exception {
            log("set", key, value);
            if (sb.closingChar=='0') {
                sb.append('(');
                setCloseChar(')');
            }
            if (first) {
                first=false;
            } else {
                sb.append(",");
            }
            sb.append(key);
            sb.append(":");
            printValue(value);
        }

        @Override
        public ListBuilder asListBuilder() throws Exception {
            checkCloseChar();
            return new ListBuilderImpl(sb);
        }

        @Override
        public MapBuilder asMapBuilder() {
            checkCloseChar();
            return new MapBuilderImpl(sb);
        }
    }
}
