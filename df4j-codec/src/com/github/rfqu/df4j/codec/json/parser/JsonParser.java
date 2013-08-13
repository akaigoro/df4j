/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.codec.json.parser;

import java.io.IOException;
import java.util.ArrayDeque;

import com.github.rfqu.df4j.codec.json.*;
import com.github.rfqu.df4j.pipeline.CharSink;

public class JsonParser implements CharSink {
    static final char LBRACE='{', RBRACE='}', LSQUARE='[', RSQUARE=']'
            , COMMA=',', COLON=':'
            , SPACE=' ', TAB='\t', NEWL='\n', QUOTE='"'
            , EOF=Character.MAX_VALUE;
    static final int stringTag=1, numTag=2, valueTag=3;
    
    final JsonBuilder out;
    ArrayDeque<Decoder> parserStack=new ArrayDeque<Decoder>();
    Scanner currentScanner;

    public JsonParser(JsonBuilder out) {
        if (out==null) {
            throw new IllegalArgumentException();
        }
        this.out = out;
        pushDecoder(new StructDecoder());
    }
    
    void pushDecoder(Scanner newScanner) {
        parserStack.push((Decoder) currentScanner);
        currentScanner=newScanner;
    }
    
    Decoder popDecoder() {
        Decoder res=parserStack.pop();
        currentScanner=res;
        return res;
    }
    
    @Override
    public void postChar(char cch) {
        /*
        switch (cch) {
        case SPACE: case TAB:   case NEWL:
            return;
        } // end switch
        */
        try {
            boolean swallowed=currentScanner.postChar(cch);
            if (!swallowed) {
                throw new IllegalArgumentException();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    
    class StructDecoder implements Decoder {
        @Override
        public boolean postChar(char ch) throws IOException {
            switch (ch) {
            case LSQUARE:
                pushDecoder(new ListDecoder());
                JsonParser.this.out.startList();
                return true;
            case LBRACE:
                pushDecoder(new SetDecoder());
                JsonParser.this.out.startSet();
                return true;
             default:
                 return false;
            }
        }

        @Override
        public void scanned(int tag, String res) throws IOException {
            // TODO Auto-generated method stub
            
        }
        
    }
    

    public boolean decodeValue(char ch) throws IOException {
            switch (ch) {
            case LSQUARE:
                pushDecoder(new ListDecoder());
                out.startList();
                return true;
            case LBRACE:
                pushDecoder(new SetDecoder());
                out.startSet();
                return true;
            case QUOTE:
                pushDecoder(new StringScanner());
                return true;
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9':
                pushDecoder(new NumScanner());
                return true;
            case 'f': case 't': case 'n':
                pushDecoder(new LiteralScanner());
                return true;
             default:
                 return false;
            }
        }
    
    class LiteralScanner implements Scanner {
        StringBuilder sb=new StringBuilder();
        
        @Override
        public boolean postChar(char ch) throws IOException {
            switch (ch) {
            case RSQUARE:
            case RBRACE:
            case COMMA:
                popDecoder().scanned(valueTag, sb.toString());
                return true;
            case EOF:
                throw new IllegalArgumentException();
            default:
                sb.append(ch);
                return true;
            }
        }
    }
    
    class StringScanner implements Scanner {
        StringBuilder sb=new StringBuilder();
        
        @Override
        public boolean postChar(char ch) throws IOException {
            if (ch==QUOTE) {
                popDecoder().scanned(stringTag, sb.toString());
                return true;
            } else if (ch==EOF) {
                throw new IllegalArgumentException();
            } else {
                 sb.append(ch);
                 return true;
            }
        }
    }
    
    class NumScanner implements Scanner {
        StringBuilder sb=new StringBuilder();
        
        @Override
        public boolean postChar(char ch) throws IOException {
            switch (ch) {
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9':
            case '+': case '-': case '.': case 'e': case 'E':
                sb.append(ch);
                return true;
            case EOF:
                throw new IllegalArgumentException();
            default:
                // collect and post number
                popDecoder().scanned(numTag, sb.toString());
                return false;
            }
        }
    }
    
    class ListDecoder implements Decoder {

        @Override
        public boolean postChar(char ch) throws IOException {
            if (ch==RSQUARE) {
                popDecoder();
                out.end();
                return true;
            } else {
                 return decodeValue(ch);
            }
        }

        @Override
        public void scanned(int tag, String res) throws IOException {
            // TODO Auto-generated method stub
            
        }
    }
    
    class SetDecoder implements Decoder {
        final int exepctKey=1, expectColon=2, expectValue=3; 
        int state=exepctKey;

        @Override
        public boolean postChar(char ch) throws IOException {
            switch (state) {
            case exepctKey:
                switch (ch) {
                case QUOTE:
                    pushDecoder(new StringScanner());
                    return true;
                default:
                    return false;
                }
            case expectColon:
                switch (ch) {
                case COLON:
                    state=expectValue;
                    return true;
                default:
                    return false;
                }
            case expectValue:
                return decodeValue(ch);
            default:
                throw new RuntimeException("internal error");
            }
        }

        @Override
        public void scanned(int tag, String res) throws IOException {
            switch (state) {
            case exepctKey:
                out.setKey(res);
                state=expectColon;
            case expectValue:
                switch (state) {
                case stringTag:
                    out.addString(res);
                    break;
                case numTag:
                    out.addInt(Integer.parseInt(res));
                    break;
                case valueTag:
                    out.addString(res); // TODO
                    break;
                 default:
                }   
            default:
                throw new RuntimeException("internal error");
            }
        }
    }
}
