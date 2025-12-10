/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.codec.binary.Hex
 */
package com.galliumdata.server.util;

import org.apache.commons.codec.binary.Hex;

public class HexUtil {
    public static byte[] decodeContinuousBytes(String s) {
        if ((s = s.replaceAll(" ", "")).length() % 2 == 1) {
            throw new RuntimeException("Byte string has odd number of characters");
        }
        try {
            return Hex.decodeHex((String)s);
        }
        catch (Exception ex) {
            throw new RuntimeException("Unable to decode hex data: " + String.valueOf(ex));
        }
    }
}
