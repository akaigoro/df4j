/*
 * Copyright 2013 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.df4j.pipeline.codec.scanner;

import java.nio.CharBuffer;
import org.df4j.pipeline.core.SinkNode;

public abstract class AbstractScanner extends SinkNode<CharBuffer> {
    public static final char 
        EOF=0, SPACE=' ', TAB='\t', NEWL='\n';
    
    protected CharRingBuffer charBuffer=new CharRingBuffer();
    protected SubScanner generalScanner;
    protected SubScanner subScanner;

    public AbstractScanner() {
        this.generalScanner=createGeneralScanner();
        subScanner=generalScanner;
    }

    protected abstract SubScanner createGeneralScanner();

    @Override
    protected void act(CharBuffer inbuf) {
        while (inbuf.hasRemaining()) {
            char c = inbuf.get();
            scan(c);
        }
    }

    @Override
    protected void complete() {
        scan(EOF);
    }

    protected void scan(char ch) {
        /*
        if (newLineSeen) {
            newLineSeen=false;
            charBuffer.startLine();
        }
        */
        switch (ch) {
            case NEWL:
                // if this chracter will cause error,
                // diagnostics should include current line
                // so postpone switching to another line for next character
                subScanner.postChar(ch);
                break;
            default:
                charBuffer.putChar(ch);
                subScanner.postChar(ch);
        }
    }

    protected void markTokenPosition() {
        charBuffer.markTokenPosition();
    }

    protected void postFirstChar(char cch, SubScanner scanner) {
        markTokenPosition();
        subScanner=scanner;
        scanner.postFirstChar(cch);            
    }

    protected void rePostChar(char cch) {
        subScanner.postChar(cch);
    }

    protected ParseException toParseException(Throwable e) {
        String header;
        if (e instanceof ParseException) {
            header="Syntax error";
        } else {
            header=e.getClass().getName();
        }
        String message = charBuffer.getTokenLine(header, e.getMessage());
        StackTraceElement[] stackTrace = e.getStackTrace();
        ParseException ee = new ParseException(message, e);
        ee.setStackTrace(stackTrace);
        return ee;
    }

    public abstract void postToken(char tokenType, String tokenString);
    
    public void postParseError(String message) {
        setParseError(new ParseException(message));
    }
    
    public abstract void setParseError(Throwable e);

    protected abstract class SubScanner {
        private StringBuilder sb=new StringBuilder();
        
        protected void postFirstChar(char cch) {
            append(cch);            
        }

        protected abstract void postChar(char ch);

        /**
         * posts token with string from string builder
         * @param tokenType
         */
        protected final void postToken(char tokenType) {
            String str=sb.toString();
            AbstractScanner.this.postToken(tokenType, str);
            sb.delete(0, str.length());
            subScanner=generalScanner;
        }

        protected final void append(char ch) {
            sb.append(ch);
        }
   }
}
