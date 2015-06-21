/*
 * Copyright 2013 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.pipeline.codec.xml.builder.impl;

import org.df4j.pipeline.codec.xml.builder.*;

public class JsonPrinter implements JsonBulderFactory {
    Printer pr=new Printer();

    @Override
    public ListBuilder newListBuilder() {
        pr.log("newListBuilder");
        return new ListBuilderImpl();
    }

    @Override
    public MapBuilder newMapBuilder() {
        pr.log("newMapBuilder");
        return new MapBuilderImpl();
    }

    static class StringWrapper {
        private String str;

        public StringWrapper(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
        
    }

    public static class MyStringBuilder {
        StringBuilder sb=new StringBuilder();
        public char closingChar='0';
        
        public void append(char ch) {
            sb.append(ch);
        }

        public void append(Object value) {
            sb.append(value);
        }

        @Override
        public String toString() {
            return sb.toString();
        }
    }
    
    public static class Printer {
        protected MyStringBuilder sb;
        
        public Printer(MyStringBuilder sb) {
            this.sb = sb;
        }

        public Printer() {
            this(new MyStringBuilder());
        }

        protected void setCloseChar(char closingChar) {
            log("setCloseChar",closingChar);
            checkCloseChar();
            sb.closingChar=closingChar;
        }

        protected void checkCloseChar() {
            log("checkCloseChar",sb.closingChar);
            if (sb.closingChar!='0') {
                sb.append(sb.closingChar);
                sb.closingChar='0';
            }
        }

        protected void printValue(Object value) {
            if (value instanceof String) {
                sb.append('"');
                sb.append(value);
                sb.append('"');
            } else {
                sb.append(value);
            }
        }

        public StringWrapper getValue() {
            checkCloseChar();
            return new StringWrapper(sb.toString());
        }

        
        public void log(String procName, Object... args) {
        	/*
            System.out.print(getClass().getSimpleName());
            System.out.print(".");
            System.out.print(procName);
            System.out.print(":");
            for (Object arg: args) {
                System.out.print(" ");
                System.out.print(arg);
            }
            System.out.println();
            */
        }
    }
    
    public class ListBuilderImpl extends Printer implements ListBuilder {
        private boolean first=true;

        public ListBuilderImpl(MyStringBuilder sb) {
            super(sb);
            sb.append('[');
            setCloseChar(']');
        }

        public ListBuilderImpl() {
            this(new MyStringBuilder());
        }

        @Override
        public void add(Object value) {
            log("add", value);
            if (first) {
                first=false;
            } else {
                sb.append(",");
            }
            printValue(value);
        }
    }
    
    public class MapBuilderImpl extends Printer implements MapBuilder {
        private boolean first=true;

        public MapBuilderImpl(MyStringBuilder sb) {
            super(sb);
            sb.append('{');
            setCloseChar('}');
        }

        public MapBuilderImpl() {
            this(new MyStringBuilder());
        }

        @Override
        public void set(String key, Object value) {
            log("set", key, value);
            if (first) {
                first=false;
            } else {
                sb.append(",");
            }
            sb.append('"');
            sb.append(key);
            sb.append('"');
            sb.append(':');
            printValue(value);
        }
    }
}
