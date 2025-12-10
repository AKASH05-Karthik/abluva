/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.util;

public class ArrayUtil {
    public static void reverseArray(byte[] a) {
        if (a == null) {
            return;
        }
        int i = 0;
        int j = a.length - 1;
        while (j > i) {
            byte tmpByte = a[j];
            a[j--] = a[i];
            a[i++] = tmpByte;
        }
    }

    public static boolean compareByteArrays(byte[] a1, byte[] a2) {
        if (a1 == null || a2 == null) {
            return false;
        }
        if (a1.length != a2.length) {
            return false;
        }
        for (int i = 0; i < a1.length; ++i) {
            if (a1[i] == a2[i]) continue;
            return false;
        }
        return true;
    }
}
