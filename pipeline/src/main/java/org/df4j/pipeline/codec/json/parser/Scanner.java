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
import java.io.EOFException;
import java.io.IOException;
import org.df4j.pipeline.codec.scanner.ParseException;

public class Scanner {
    protected static final int LPAREN='(', RPAREN=')', LBRACE='{', RBRACE='}'
            , LBRACKET='[', RBRACKET=']', COMMA=',', COLON=':'
            , SPACE=' ', TAB='\t', NEWL='\n', QUOTE='"', QUOTE2='\'', COMMENT='#'
            , EOF=Character.MAX_VALUE
            , NUMBER=EOF+1, IDENT=NUMBER+1, STRING=IDENT+1;
    
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

    private BufferedReader in;
    private String line;
    private int lineNumber=0;
    private int pos=0;
    private char cch;
    
    private String tokenLine;
    private int tokenLineNumber;
    private int tokenPos;
    protected int tokenType;
    protected String tokenString;
    
    public void setReader(BufferedReader in) throws IOException {
        this.in = in;
        // skip empty lines
        for (;;) {
            line = in.readLine();
            lineNumber++;
            if (line == null) {
                throw new EOFException("source file is empty");
            }
            if (line.length()>0) {
                break;
            }
        }
        pos=0;
        cch=line.charAt(pos);
    }

    protected ParseException toParseException(Exception e) throws ParseException {
        StringBuilder sb=new StringBuilder("\n");
        if (e instanceof ParseException) {
            sb.append("Syntax error");
        } else {
            sb.append(e.getClass().getName());
        }
        sb.append(" at line ").append(tokenLineNumber).append(":\n")
          .append(tokenLine).append("\n");
        for (int k=0; k<tokenPos; k++) {
            sb.append(' ');
        }
        sb.append("^ ").append(e.getMessage());
        String message = sb.toString();
        StackTraceElement[] stackTrace = e.getStackTrace();
        ParseException ee = new ParseException(message, e);
        ee.setStackTrace(stackTrace);
        return ee;
    }
    
    protected void skipSpaces() throws IOException, ParseException {
        for (;;) {
            switch (tokenType) {
            case SPACE:
            case TAB:
            case NEWL:
                scan();
                break;
            default:
                return;
            }
        }
    }

    protected void checkAndScan(int expected) throws IOException, ParseException {
        if (tokenType!=expected) {
            throw new ParseException(token2Str(expected) +" expected, but '"+token2Str(tokenType)+"' seen");
        }
        scan();
    }
    
    /**
     * 
     * @return next character
     * @throws IOException
     */
    private void nextChar() throws IOException {
        pos++;
        if (pos==line.length()) {
            cch=NEWL;
            return;
        }
        if (pos>line.length()) {
            line=in.readLine();
            lineNumber++;
            pos=0;
            if (line==null) {
                cch=EOF;
                return;
            } 
            if (line.length()==0) {
                cch=NEWL;
                return;
            } 
        }
        cch=line.charAt(pos);
    }
    
    /**
     * determines next lexical token type
     */
    protected int lookahead() throws IOException, ParseException {
        for (;;) {
            switch (cch) {
            case SPACE: case TAB:   case NEWL:
                nextChar();
                continue;
                /*
            case COMMENT: // TODO
                nextChar();
                continue;
                */
            case EOF:
            case LPAREN: case RPAREN:
            case LBRACE: case RBRACE: case LBRACKET:
            case RBRACKET: case COMMA: case COLON:
            case QUOTE: case QUOTE2:
                return cch;
            default:
                if (Character.isDigit(cch)) {
                    return NUMBER;
                } else if (Character.isLetter(cch)) {
                    return IDENT;
                } else {
                    throw new ParseException("unexpected character:"+cch);
                }
            } // end switch
        } //end for loop
    }

    /**
     * determines current lexical token
     * @return token type, equal to the start character, or alpha for literal
     * @throws IOException
     * @throws ParseException 
     */
    protected void scan() throws IOException, ParseException {
        for (;;) {
            switch (cch) {
            case EOF:
                tokenString=null;
                tokenType=cch;
                return;
            case LPAREN: case RPAREN:
            case LBRACE: case RBRACE: case LBRACKET:
            case RBRACKET: case COMMA: case COLON:
                markTokenPosition();
                tokenString=null;
                tokenType=cch;
                nextChar();
                return;
            case SPACE: case TAB:   case NEWL:
                nextChar();
                continue;
            case COMMENT:
                pos=line.length()-1;
                nextChar();
                continue;
            case QUOTE:
            case QUOTE2:
                scanString(cch);
                return;
            default:
                if (Character.isDigit(cch)) {
                    scanNumber();
                    return;
                } else if (Character.isLetter(cch)) {
                    scanIdent();
                    return;
                } else {
                    throw new ParseException("unexpected character:"+cch);
                }
            } // end switch
        } //end for loop
    }

    private void markTokenPosition() {
        tokenLineNumber=lineNumber;
        tokenLine=line;
        tokenPos=pos;
    }

    /** parses quoted literal
     * 
     * @param quoteSymbol
     * @throws IOException
     */
    private void scanString(int quoteSymbol) throws IOException {
        markTokenPosition();
        StringBuilder sb=new StringBuilder();
        scan_alpha:
        for (;;) {
            nextChar();
            if (cch==quoteSymbol) {
                nextChar();
                break scan_alpha;
            } else if (cch==EOF) {
                break scan_alpha;
            }
            sb.append(cch);
        } // end for loop
        tokenString=sb.toString();
        tokenType=STRING;
    }
    
    /**
     * parses literal
     * @throws IOException
     */
    private void scanNumber() throws IOException {
        markTokenPosition();
        StringBuilder sb=new StringBuilder();
        scan_alpha:
        for (;;) {
            sb.append(cch);
            nextChar();
            switch (cch) {
            case EOF:
            case LPAREN: case RPAREN:
            case LBRACE: case RBRACE: case LBRACKET:
            case RBRACKET: case COMMA: case COLON:
            case SPACE: case TAB:   case NEWL:
            case COMMENT:
                break scan_alpha;
            }
        } // end for loop
        tokenString=sb.toString();
        // TODO pares number
        tokenType=NUMBER;
    }
    
    /**
     * parses literal
     * @throws IOException
     */
    private void scanIdent() throws IOException {
        markTokenPosition();
        StringBuilder sb=new StringBuilder();
        for (;;) {
            sb.append(cch);
            nextChar();
            if (!Character.isDigit(cch) && !Character.isLetter(cch)) {
                break;
            }
        } // end for loop
        tokenString=sb.toString();
        // TODO embedded values
        tokenType=IDENT;
    }
}
