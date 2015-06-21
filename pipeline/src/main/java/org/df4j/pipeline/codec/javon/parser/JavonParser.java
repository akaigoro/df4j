/*
 * Copyright 2013 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.pipeline.codec.javon.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import org.df4j.pipeline.codec.javon.builder.JavonBulderFactory;
import org.df4j.pipeline.codec.javon.builder.ObjectBuilder;
import org.df4j.pipeline.codec.json.parser.JsonParser;
import org.df4j.pipeline.codec.scanner.ParseException;

/** at least one of following methods should be overriden:
 * newRootObject(),
 * newRootList()
 * 
 * @author ak
 *
 */
public class JavonParser extends JsonParser {
	protected JavonBulderFactory factory; 

    public JavonParser(JavonBulderFactory factory) {
        super(factory);
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
            case IDENT:
                res=parseObject();
                break;
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

    protected Object parseObject() throws Exception {
        ObjectBuilder builder = factory.newObjectBuilder(tokenString);
        checkAndScan(IDENT);
        if (tokenType == LPAREN) {
            scan();
            ArrayList<Object> args = null;
            positionalArgs: // parse positional parameters
            for (;;) {
                switch (tokenType) {
                case RPAREN:
                    // do not eat
                    break positionalArgs;
                case IDENT: {
                    // lookahead needed to differentiate A and A:A
                    if (lookahead() == COLON) {
                        break positionalArgs;
                    }
                    break;
                }
                case COMMA:
                    scan();
                    continue;
                }
                Object value = parseValue();
                if (args==null) {
                    args = new ArrayList<Object>();
                }
                args.add(value);
            }
            if (args==null) {
                builder.instatntiate();
            } else {
                builder.instatntiate(args.toArray());
            }
            // parse named parameters
            namedArgs:
            for (;;) {
                switch (tokenType) {
                case RPAREN:
                    scan();
                    break namedArgs;
                case COMMA:
                    scan();
                    break;
                case IDENT: {
                    String key = tokenString;
                    scan();
                    checkAndScan(COLON);
                    Object value = parseValue();
                    builder.set(key, value);
                    break;
                }
                default:
                    throw new ParseException("comma  or ) expected");
                }
            }
        } else {
            builder.instatntiate();
        }
        if (tokenType == LBRACKET) {
            parseList(builder.asListBuilder());
        }
        if (tokenType == LBRACE) {
            parseMap(builder.asMapBuilder());
        }
        return builder.getValue();
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
            res=parseObject();
        }
        return res;
    }
}

