/*
 * Copyright 2013 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.pipeline.codec.json.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import org.df4j.pipeline.codec.json.builder.JsonBulderFactory;
import org.df4j.pipeline.codec.json.builder.ListBuilder;
import org.df4j.pipeline.codec.json.builder.MapBuilder;
import org.df4j.pipeline.codec.scanner.ParseException;

/** at least one of following methods should be overriden:
 * newRootObject(),
 * newRootList()
 * 
 * @author ak
 *
 */
public class JsonParser extends Scanner {
    protected final JsonBulderFactory factory; 

    public JsonParser(JsonBulderFactory factory) {
        this.factory = factory;
    }

    public Object parseFrom(Reader ir) throws IOException, ParseException {
        setReader(new BufferedReader(ir));
        scan();
        // skip empty lines
        skipSpaces();
        Object res;
        try {
            switch (tokenType) {
            case LBRACKET:
                res=parseList();
                break;
            case LBRACE:
                res=parseMap();
                break;
            default:
                throw new ParseException("Identifier, { or [ expected");
            }
        } catch (Exception e) {
            throw toParseException(e);
        }
        // skip empty lines
        skipSpaces();
        if (tokenType!=EOF ) {
            throw new ParseException("extra text:"+tokenType);
        }
        return res;
    }
    
    public Object parseFrom(InputStream is) throws IOException, ParseException {
        return parseFrom(new InputStreamReader(is));
    }

    public Object parseFrom(File inputFile) throws IOException, ParseException {
        return parseFrom(new BufferedReader(new FileReader(inputFile)));
    }

    public Object parseFrom(String str) throws IOException, ParseException {
        return parseFrom(new StringReader(str));
    }

    protected Object parseMap() throws IOException, ParseException, Exception {
        MapBuilder builder = factory.newMapBuilder();
        parseMap(builder);
        return builder.getValue();
    }

    protected void parseMap(MapBuilder builder) throws IOException, ParseException, Exception {
        checkAndScan(LBRACE);
        for (;;) {
            switch (tokenType) {
            case COMMA:
                scan();
                break;
            case RBRACE:
                scan();
                return;
            case STRING: 
            case IDENT: {
                String key = tokenString;
                scan();
                checkAndScan(COLON);
                Object value=parseValue();
                builder.set(key, value);
                break;
            }
            default:
                throw new ParseException("comma  or } expected");
            }
        }
    }

    /**
     * allows spare commas
     */
    protected void parseList(ListBuilder builder) throws Exception {
        checkAndScan(LBRACKET);
        for (;;) {
            switch (tokenType) {
            case RBRACKET:
                scan();
                return;
            case COMMA:
                scan();
                break;
            default:
                Object value=parseValue();
                builder.add(value);
            }
        }
    }

    protected Object parseList() throws Exception {
        ListBuilder builder = factory.newListBuilder();
        parseList(builder);
        return builder.getValue();
    }
    
    /** 
     * string, array, map, or object
     */
    protected Object parseValue() throws Exception {
        switch (tokenType) {
        case LBRACKET:
            return parseList();
        case LBRACE:
            return parseMap();
        case STRING: {
            String str=tokenString;
            scan();
            return str;
        }
        case NUMBER: 
            return parseNumber();
        case IDENT:
            return parseIdent();
        default: 
            throw new ParseException("value expected, but "+token2Str(tokenType)+" seen");
        }
    }
    
    protected Object parseNumber() throws ParseException, IOException {
        Object res;
        try {
            res=Integer.valueOf(tokenString);
        } catch (NumberFormatException e) {
            try {
                res=Double.valueOf(tokenString);
            } catch (NumberFormatException e1) {
                throw new ParseException("bad number:"+tokenString);
            }
        }
        scan();
        return res;
    }
    
    /** 
     * string, array, or object
     * @return
     * @throws IOException
     * @throws ParseException 
     */
    protected Object parseIdent() throws Exception {
        Object res;
        if ("null".equals(tokenString)) {
            res=null;
            scan();
        } else if ("false".equals(tokenString)) {
            res=Boolean.FALSE;
            scan();
        } else if ("true".equals(tokenString)) {
            res=Boolean.TRUE;
            scan();
        } else {
            throw new ParseException("invalid identifier");
        }
        return res;
    }
}

