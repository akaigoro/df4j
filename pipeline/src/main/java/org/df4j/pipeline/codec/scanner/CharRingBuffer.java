package org.df4j.pipeline.codec.scanner;

/** save latest chars for diagnostic properties.
 */
class CharRingBuffer {
    static int LEN=64;
    private char[] buf=new char[LEN];
    private int lineNumber=0;
    private int count=0;
    
    void putChar(char ch) {
        buf[count%LEN]=ch;
        count++;
    }

    void startLine() {
        count=0;
        lineNumber++;
    }
    
    private int tokenLineNumber;
    private int tokenCount;
    
    void markTokenPosition() {
        tokenLineNumber=lineNumber;
        tokenCount=count;
    }
    
    String getTokenLine(String header, String message) {
        StringBuilder sb = new StringBuilder();
        sb.append(" at line ").append(tokenLineNumber).append(":\n");
        tokenCount = Math.max(count-tokenCount, LEN)-count;
        for (int pos=tokenCount; pos<count; pos++) {
            sb.append(buf[pos%LEN]);
        }
        sb.append("\n");
        for (int pos=tokenCount; pos<count; pos++) {
            sb.append(' ');
        }
        sb.append("^ ").append(message);
        return sb.toString();
    }
}