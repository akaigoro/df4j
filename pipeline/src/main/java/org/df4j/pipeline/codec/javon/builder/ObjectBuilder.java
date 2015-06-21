/*
 * Copyright 2013 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.pipeline.codec.javon.builder;

import org.df4j.pipeline.codec.json.builder.*;
import org.df4j.pipeline.codec.scanner.ParseException;

public interface ObjectBuilder extends StructBuilder {
    public void instatntiate() throws Exception;
    public void instatntiate(Object... args) throws Exception;
    public boolean isInstantiated();
    public void set(String key, Object value) throws Exception;
    public ListBuilder asListBuilder() throws Exception;
    public MapBuilder asMapBuilder() throws ParseException;
}
