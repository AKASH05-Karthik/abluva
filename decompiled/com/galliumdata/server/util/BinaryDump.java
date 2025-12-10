/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.util;

public class BinaryDump {
    private static final char[] hexChars = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static String getBinaryDump(byte[] bytes) {
        return BinaryDump.getBinaryDump(bytes, 0, bytes.length);
    }

    public static String getBinaryDump(byte[] bytes, int offset, int length) {
        if (length == 0) {
            return "[empty array]";
        }
        int idx = offset;
        StringBuffer sb = new StringBuffer();
        while (idx - offset < length) {
            int rowIdx;
            StringBuffer asciiSb = new StringBuffer();
            for (rowIdx = 0; rowIdx < 16 && idx + rowIdx - offset < length; ++rowIdx) {
                BinaryDump.addByte(sb, asciiSb, bytes[idx + rowIdx]);
            }
            idx += rowIdx;
            sb.append("   ".repeat(Math.max(0, 16 - rowIdx)));
            sb.append("  ");
            sb.append(asciiSb);
            sb.append('\n');
        }
        return sb.toString();
    }

    private static void addByte(StringBuffer sb, StringBuffer sb2, byte b) {
        int idx = b;
        if (idx < 0) {
            idx = 256 + idx;
        }
        sb.append(hexChars[idx / 16]);
        sb.append(hexChars[idx % 16]);
        sb.append(' ');
        if (idx >= 32 && idx <= 126) {
            sb2.append((char)idx);
        } else {
            sb2.append('.');
        }
    }
}
