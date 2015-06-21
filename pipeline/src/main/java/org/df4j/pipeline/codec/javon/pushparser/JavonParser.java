/*
 * Copyright 2013 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.pipeline.codec.javon.pushparser;

import java.util.ArrayList;
import org.df4j.pipeline.codec.javon.builder.JavonBulderFactory;
import org.df4j.pipeline.codec.javon.builder.ObjectBuilder;
import org.df4j.pipeline.codec.json.builder.ListBuilder;
import org.df4j.pipeline.codec.json.builder.MapBuilder;
import org.df4j.pipeline.codec.json.pushparser.JsonParser;
import org.df4j.pipeline.codec.scanner.ParseException;

public class JavonParser extends JsonParser {
    JavonBulderFactory factory;

    public JavonParser(JavonBulderFactory factory) {
        super(factory);
        this.factory = factory;
        new RootTokenPort();
    }

    protected void setCurrentParser(Parser tp) {
        currentParser=tp;
    }

    protected void parseObject(String tokenString) {
        try {
            ObjectBuilder builder = factory.newObjectBuilder(tokenString);
            new ObjectParser(currentParser, builder);
        } catch (Exception e) {
            currentParser.postParseError(e.getMessage());
        }
    }

    /**
     * null, boolean, or object
     */
    protected void parseIdent(String tokenString) {
        if ("null".equals(tokenString)) {
            currentParser.setValue(null);
        } else if ("false".equals(tokenString)) {
            currentParser.setValue(Boolean.FALSE);
        } else if ("true".equals(tokenString)) {
            currentParser.setValue(Boolean.TRUE);
        } else {
            parseObject(tokenString);
        }
    }

    class RootTokenPort extends JsonParser.RootTokenPort {

        @Override
		protected void firstToken(int tokenType, String tokenString) {
            switch (tokenType) {
            case IDENT:
                parseObject(tokenString);
                break;
            case LBRACKET:
                parseList();
                break;
            case LBRACE:
                parseMap();
                break;
            default:
                postParseError("Identifier, { or [ expected");
            }
		}
    }
    
    class ObjectParser extends Parser {
        ObjectBuilder builder;
        ArrayList<Object> args = null;
        String key;
        int state = 0;

        public ObjectParser(Parser parent, ObjectBuilder builder) {
            super(parent);
            this.builder = builder;
        }

        @Override
		public void postToken(char tokenType, String tokenString) {
			switch (state) {
			case 0: // just after class name
				switch (tokenType) {
				case LPAREN:
					state = 1;
					break;
				default: // object without arguments
					state = 6;
					instantiate();
					postToken(tokenType, tokenString);
				}
				break;
			case 1: // right after '(' parse positional arguments
				switch (tokenType) {
				case RPAREN: // end args
					state = 6;
					instantiate();
					break;
				case IDENT: // ambiguity detected
					key = tokenString;
					state = 2;
					break;
				case COMMA:
					break;
				default:
					parseValue(tokenType, tokenString);
				}
				break;
			case 2: // resolve ambiguity a, or a:v
				if (tokenType == COLON) {
					instantiate();
					state = 5; // go to named args
				} else {
                    state=1; // back to positional args
					parseIdent(key);
					currentParser.postToken(tokenType, tokenString);
				}
				break;
			// parse named arguments
			case 3: // parse key in key-value pair
				switch (tokenType) {
				case RPAREN: // end args
					state = 6;
					break;
				case COMMA:
					break;
				case IDENT:
					key = tokenString;
					state = 4;
					break;
				default:
					postParseError("comma  or ) expected");
				}
				break;
			case 4: // check ':'
				if (tokenType == COLON) {
					state = 5;
				} else {
					postParseError("':' expected");
				}
				break;
			case 5: // parse value in key-value pair
				parseValue(tokenType, tokenString);
				break;
			case 6: // parse list and map tails
				switch (tokenType) {
				case LBRACKET:
					try {
						ListBuilder listBuilder = builder.asListBuilder();
						new ListParser(this, listBuilder);
					} catch (Exception e) {
					    setParseError(e);
					}
					break;
				case LBRACE:
					try {
						MapBuilder mapBuilder = builder.asMapBuilder();
						new MapParser(this, mapBuilder);
					} catch (ParseException e) {
					    setParseError(e);
					}
					break;
				default:
					returnValue(builder.getValue());
					currentParser.postToken(tokenType, tokenString);
				}
				break;
			}
		}

        @Override
        public void setValue(Object value) {
            try {
                switch (state) {
                case 1: // after '(' parse positional arguments
                    if (args==null) {
                        args = new ArrayList<Object>();
                    }
                    args.add(value);
                    break;
                case 5: // parse value in key-value pair
                    builder.set(key, value);
                    state=3;
                    break;
                case 6: // when processing list and map tails, ListParser or MapParser returns
                    break;
                default:
                    postParseError("internal error: call to setValue when state="+state);
                }
            } catch (Exception e) {
                setParseError(e);
            }
        }

        protected void instantiate() {
            if (builder.isInstantiated()) {
                return;
            }
            try {
                if (args==null) {
                    builder.instatntiate();
                } else {
                    builder.instatntiate(args.toArray());
                    args=null;
                }
            } catch (Exception e) {
                setParseError(e);
            }
        }
    }
}
