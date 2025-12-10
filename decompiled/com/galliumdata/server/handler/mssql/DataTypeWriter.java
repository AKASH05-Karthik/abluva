/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.handler.mssql;

public class DataTypeWriter {
    public static void encodeTwoByteInteger(byte[] buffer, int offset, short num) {
        buffer[offset + 0] = (byte)(num >> 8 & 0xFF);
        buffer[offset + 1] = (byte)(num & 0xFF);
    }

    public static void encodeTwoByteIntegerLow(byte[] buffer, int offset, short num) {
        buffer[offset + 0] = (byte)(num & 0xFF);
        buffer[offset + 1] = (byte)(num >> 8 & 0xFF);
    }

    public static void encodeFourByteInteger(byte[] buffer, int offset, int num) {
        buffer[offset + 0] = (byte)(num >> 24 & 0xFF);
        buffer[offset + 1] = (byte)(num >> 16 & 0xFF);
        buffer[offset + 2] = (byte)(num >> 8 & 0xFF);
        buffer[offset + 3] = (byte)(num & 0xFF);
    }

    public static void encodeFourByteIntegerLow(byte[] buffer, int offset, int num) {
        buffer[offset + 3] = (byte)(num >> 24 & 0xFF);
        buffer[offset + 2] = (byte)(num >> 16 & 0xFF);
        buffer[offset + 1] = (byte)(num >> 8 & 0xFF);
        buffer[offset + 0] = (byte)(num & 0xFF);
    }

    public static void encodeEightByteInteger(byte[] buffer, int offset, long num) {
        buffer[offset + 0] = (byte)(num >> 56 & 0xFFL);
        buffer[offset + 1] = (byte)(num >> 48 & 0xFFL);
        buffer[offset + 2] = (byte)(num >> 40 & 0xFFL);
        buffer[offset + 3] = (byte)(num >> 32 & 0xFFL);
        buffer[offset + 4] = (byte)(num >> 24 & 0xFFL);
        buffer[offset + 5] = (byte)(num >> 16 & 0xFFL);
        buffer[offset + 6] = (byte)(num >> 8 & 0xFFL);
        buffer[offset + 7] = (byte)(num & 0xFFL);
    }

    public static void encodeEightByteIntegerLow(byte[] buffer, int offset, long num) {
        buffer[offset + 7] = (byte)(num >> 56 & 0xFFL);
        buffer[offset + 6] = (byte)(num >> 48 & 0xFFL);
        buffer[offset + 5] = (byte)(num >> 40 & 0xFFL);
        buffer[offset + 4] = (byte)(num >> 32 & 0xFFL);
        buffer[offset + 3] = (byte)(num >> 24 & 0xFFL);
        buffer[offset + 2] = (byte)(num >> 16 & 0xFFL);
        buffer[offset + 1] = (byte)(num >> 8 & 0xFFL);
        buffer[offset + 0] = (byte)(num & 0xFFL);
    }

    public static void encodeEightByteDecimal(byte[] buffer, int offset, long num) {
        buffer[offset + 7] = (byte)(num >> 24 & 0xFFL);
        buffer[offset + 6] = (byte)(num >> 16 & 0xFFL);
        buffer[offset + 5] = (byte)(num >> 8 & 0xFFL);
        buffer[offset + 4] = (byte)(num >> 0 & 0xFFL);
        buffer[offset + 3] = (byte)(num >> 56 & 0xFFL);
        buffer[offset + 2] = (byte)(num >> 48 & 0xFFL);
        buffer[offset + 1] = (byte)(num >> 40 & 0xFFL);
        buffer[offset + 0] = (byte)(num >> 32 & 0xFFL);
    }

    public static void encodeEightByteNumber(byte[] buffer, int offset, long num) {
        buffer[offset + 0] = (byte)(num >> 0 & 0xFFL);
        buffer[offset + 1] = (byte)(num >> 8 & 0xFFL);
        buffer[offset + 2] = (byte)(num >> 16 & 0xFFL);
        buffer[offset + 3] = (byte)(num >> 24 & 0xFFL);
        buffer[offset + 4] = (byte)(num >> 32 & 0xFFL);
        buffer[offset + 5] = (byte)(num >> 40 & 0xFFL);
        buffer[offset + 6] = (byte)(num >> 48 & 0xFFL);
        buffer[offset + 7] = (byte)(num >> 56 & 0xFFL);
    }
}
