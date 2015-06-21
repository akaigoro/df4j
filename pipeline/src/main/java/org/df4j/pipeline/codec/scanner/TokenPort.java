/*
 * Copyright 2013 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this filbstracte except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.df4j.pipeline.codec.scanner;

/**
 * output port for characters
 */
public abstract class TokenPort {
    public abstract void postToken(char tokenType, String tokenString);
    
    public void postParseError(String message) {
        setParseError(new ParseException(message));
    }
    
    public abstract void setParseError(Throwable e);
}
