package com.github.rfqu.df4j.testutil;

public class Utils {
    public static boolean byteArraysEqual(byte[] a1, byte[] a2) {
        if (a1==a2) {
            return true;
        } else if (a1==null) {
            return false;
        } else if (a1.length!=a2.length) {
            return false;
        }
        for (int k=0; k<a1.length; k++) {
            if (a1[k]!=a2[k]) {
                return false;
            }
        }
        return true;
    }
}
