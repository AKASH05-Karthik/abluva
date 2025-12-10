/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.primitives.Ints
 *  com.google.common.primitives.Longs
 *  com.google.common.primitives.Shorts
 */
package com.galliumdata.server.handler.mssql;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.primitives.Shorts;

public class DataTypeReader {
    public static int getUnsignedByte(byte b) {
        return b & 0xFF;
    }

    public static byte readByte(byte[] bytes, int offset) {
        return (byte)(bytes[offset] & 0xFF);
    }

    public static short readTwoByteInteger(byte[] bytes, int offset) {
        return Shorts.fromBytes((byte)bytes[offset + 0], (byte)bytes[offset + 1]);
    }

    public static short readTwoByteIntegerLow(byte[] bytes, int offset) {
        return Shorts.fromBytes((byte)bytes[offset + 1], (byte)bytes[offset + 0]);
    }

    public static int readFourByteInteger(byte[] bytes, int offset) {
        return Ints.fromBytes((byte)bytes[offset + 0], (byte)bytes[offset + 1], (byte)bytes[offset + 2], (byte)bytes[offset + 3]);
    }

    public static int readFourByteIntegerLow(byte[] bytes, int offset) {
        return Ints.fromBytes((byte)bytes[offset + 3], (byte)bytes[offset + 2], (byte)bytes[offset + 1], (byte)bytes[offset + 0]);
    }

    public static long readUnsignedFourByteIntegerLow(byte[] bytes, int offset) {
        return Longs.fromBytes((byte)0, (byte)0, (byte)0, (byte)0, (byte)bytes[offset + 3], (byte)bytes[offset + 2], (byte)bytes[offset + 1], (byte)bytes[offset + 0]);
    }

    public static long readEightByteInteger(byte[] bytes, int offset) {
        return Longs.fromBytes((byte)bytes[offset + 0], (byte)bytes[offset + 1], (byte)bytes[offset + 2], (byte)bytes[offset + 3], (byte)bytes[offset + 4], (byte)bytes[offset + 5], (byte)bytes[offset + 6], (byte)bytes[offset + 7]);
    }

    public static long readEightByteIntegerLow(byte[] bytes, int offset) {
        return Longs.fromBytes((byte)bytes[offset + 7], (byte)bytes[offset + 6], (byte)bytes[offset + 5], (byte)bytes[offset + 4], (byte)bytes[offset + 3], (byte)bytes[offset + 2], (byte)bytes[offset + 1], (byte)bytes[offset + 0]);
    }

    public static long readEightByteDecimal(byte[] bytes, int offset) {
        return Longs.fromBytes((byte)bytes[offset + 3], (byte)bytes[offset + 2], (byte)bytes[offset + 1], (byte)bytes[offset + 0], (byte)bytes[offset + 7], (byte)bytes[offset + 6], (byte)bytes[offset + 5], (byte)bytes[offset + 4]);
    }
}
