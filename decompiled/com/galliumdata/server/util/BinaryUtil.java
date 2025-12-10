/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.io.BaseEncoding
 */
package com.galliumdata.server.util;

import com.galliumdata.server.handler.db2.packets.DataTypeWriter;
import com.google.common.io.BaseEncoding;

public class BinaryUtil {
    public static void reverseByteArray(byte[] bytes) {
        int len = bytes.length;
        for (int i = 0; i < len / 2; ++i) {
            byte b = bytes[i];
            bytes[i] = bytes[len - 1 - i];
            bytes[len - 1 - i] = b;
        }
    }

    public static String getHexRepresentationOfBytes(byte[] bytes) {
        return BaseEncoding.base16().lowerCase().encode(bytes);
    }

    public static String getHexRepresentationOfByte(byte b) {
        return "0x" + BaseEncoding.base16().lowerCase().encode(new byte[]{b});
    }

    public static String getHexRepresentationOfShort(short i) {
        byte[] bytes = new byte[2];
        DataTypeWriter.encodeTwoByteInteger(bytes, 0, i);
        return "0x" + BaseEncoding.base16().lowerCase().encode(bytes);
    }

    public static String getHexRepresentationOfInt(int i) {
        byte[] bytes = new byte[4];
        DataTypeWriter.encodeFourByteInteger(bytes, 0, i);
        return "0x" + BaseEncoding.base16().lowerCase().encode(bytes);
    }

    public static String getHexRepresentationOfBytes(byte[] bytes, int offset, int len) {
        byte[] buf = new byte[len];
        System.arraycopy(bytes, offset, buf, 0, len);
        return BinaryUtil.getHexRepresentationOfBytes(buf);
    }

    public static byte[] getBytesFromHexString(String s) {
        return BaseEncoding.base16().lowerCase().decode((CharSequence)s);
    }
}
