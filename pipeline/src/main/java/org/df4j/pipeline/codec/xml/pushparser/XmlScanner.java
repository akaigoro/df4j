/*
 * Copyright 2013 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.pipeline.codec.xml.pushparser;

import org.df4j.pipeline.codec.json.pushparser.JsonScanner;

public abstract class XmlScanner extends JsonScanner {
    public static final char 
            LABRACKET='<', RABRACKET='>'
            , QUESTMARK='?', EXCLMARK='!';
    
    protected SubScanner createGeneralScanner(){
        return new GeneralScanner();
    }

    private final class GeneralScanner extends SubScanner {

        /**
         * determines current lexical token
         */
        @Override
        protected void postChar(char cch) {
            switch (cch) {
            case RABRACKET:  case LABRACKET:
            case RBRACKET:  case LBRACKET:
            case COMMA: case COLON: case EOF:
                XmlScanner.this.postToken(cch, null);
                return;
            case SPACE: case TAB: case NEWL:
                return;
            case QUOTE:
            case QUOTE2:
                XmlScanner.this.postFirstChar(cch, stringScanner);
                return;
            default:
                if (Character.isDigit(cch)) {
                    XmlScanner.this.postFirstChar(cch, numScanner);
                    return;
                } else if (Character.isLetter(cch)) {
                    XmlScanner.this.postFirstChar(cch, identScanner);
                    return;
                } else {
                    postParseError("unexpected character:'"+cch+"'");
                }
            } // end switch
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
