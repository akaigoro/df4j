/*
 * Copyright 2011 by Alexei Kaigorodov
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.rfqu.df4j.codec.chars;

import com.github.rfqu.df4j.codec.BytePort;
import com.github.rfqu.df4j.codec.CharPort;

public class Byte2UTF8 implements BytePort {
    final CharPort out;
    int num; // of cont bytes
    int data;

    public Byte2UTF8(CharPort out) {
        if (out==null) {
            throw new IllegalArgumentException();
        }
        this.out = out;
    }

    BytePort leadingByteDecoder=new BytePort(){
        /** 1 - 0
         *  2 — 110
         *  3 — 1110
         */
        public void postByte(int b) {
            int n=zeroPos(b);
            if (n==1) {
                // 1 byte
                out.postChar((char) b);
            } else {
                num=n-2;
                data=b&(0xFF>>n);
                decoder=continuationByteDecoder;
            }
        }
        
    };
    
    BytePort continuationByteDecoder=new BytePort(){

        @Override
        public void postByte(int b) {
            data = (data<<6) | (b&0x3F);
            if (num > 1) {
                num--;
            } else {
                out.postChar((char) data);
                decoder=leadingByteDecoder;
            }
        }
    };
    
    BytePort decoder=leadingByteDecoder;

    @Override
    public void postByte(int b) {
        decoder.postByte(b);
    }
    
    /** counted from left to right, started from 1
     * @param b
     * @return position of zero bit
     */
    static int zeroPos(int b) {
        for (int k = 1; k <7; k++) {
            int mask = 0x100 >> k;
            if ((b & mask) == 0) {
                return k;
            }
        }
        return 7;
    }
}
