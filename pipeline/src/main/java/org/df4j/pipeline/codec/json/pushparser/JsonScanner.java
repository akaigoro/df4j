/*
 * Copyright 2013 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.pipeline.codec.json.pushparser;

import org.df4j.pipeline.codec.scanner.AbstractScanner;

public abstract class JsonScanner extends AbstractScanner {
    public static final char LPAREN='(', RPAREN=')', LBRACE='{', RBRACE='}'
            , LBRACKET='[', RBRACKET=']', COMMA=',', COLON=':'
            , SPACE=' ', TAB='\t', NEWL='\n', QUOTE='"', QUOTE2='\'', COMMENT='#'
            , EOF=0
            , NUMBER=EOF+1, IDENT=NUMBER+1, STRING=IDENT+1;
    
    private boolean newLineSeen=false;
    public StringScanner stringScanner=new StringScanner();
    public NumScanner numScanner=new NumScanner();
    public IdentScanner identScanner=new IdentScanner();
    
    protected SubScanner createGeneralScanner(){
        return new GeneralScanner();
    }

    private final class GeneralScanner extends SubScanner {

        @Override
        protected
        void postFirstChar(char cch) {
            throw new IllegalStateException();
        }

        /**
         * determines current lexical token
         */
        @Override
        protected
        void postChar(char cch) {
            switch (cch) {
            case LPAREN: case RPAREN:
            case LBRACE: case RBRACE: case LBRACKET:
            case RBRACKET: case COMMA: case COLON: case EOF:
                markTokenPosition();
                JsonScanner.this.postToken(cch, null);
                return;
            case SPACE: case TAB: case NEWL:
                return;
            case QUOTE:
            case QUOTE2:
                JsonScanner.this.postFirstChar(cch, stringScanner);
                return;
            default:
                if (Character.isDigit(cch)) {
                    JsonScanner.this.postFirstChar(cch, numScanner);
                    return;
                } else if (Character.isLetter(cch)) {
                    JsonScanner.this.postFirstChar(cch, identScanner);
                    return;
                } else {
                    markTokenPosition();
                    postParseError("unexpected character:'"+cch+"'");
                }
            } // end switch
        }
    }
    
    /** parses quoted literal
     */
    public final class StringScanner extends SubScanner {
        char quoteSymbol;
        
        protected void postFirstChar(char quoteSymbol) {
            this.quoteSymbol=quoteSymbol;            
        }

        @Override
        protected
        void postChar(char cch) {
            if (cch==quoteSymbol) {
                postToken(STRING);
            } else if (cch==EOF) {
                postParseError("unexpected end of file");
            } else {
                append(cch);
            }
        }
        
    }
    
    /** parses unquoted identifier
     * 
     * @param quoteSymbol
     */
    public final class IdentScanner extends SubScanner {
        
        @Override
        protected
        void postChar(char cch) {
            if (Character.isDigit(cch) || Character.isLetter(cch)) {
                append(cch);
            } else {
                postToken(IDENT);
                rePostChar(cch);
            }
        }
    }
    
    /** parses number
     */
    public final class NumScanner extends SubScanner {

        @Override
        protected
        void postChar(char cch) {
            switch (cch) {
            case LPAREN: case RPAREN:
            case LBRACE: case RBRACE: case LBRACKET:
            case RBRACKET: case COMMA: case COLON:
            case SPACE: case TAB:   case NEWL:
            case COMMENT:
            case EOF:
                postToken(NUMBER);
                rePostChar(cch);
                break;
            default:
                append(cch);
            }
        }   
    }

    protected static String token2Str(int t) {
        switch(t) {
        case SPACE: return "<SPACE>";
        case TAB: return "<TAB>";
        case NEWL: return "<\n>";
        case QUOTE: return "<\\\">";
        case QUOTE2: return "<'>";
        case COMMENT: return "#";
        case EOF: return "<EOF>";
        case IDENT: return "<IDENTIFIER>";
        case STRING: return "<STRING>";
        default:
           return String.valueOf((char)t);
        }
    }
}
