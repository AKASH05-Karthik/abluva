/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.handler.mssql;

public class UnableToParseException
extends RuntimeException {
    private final String packetType;

    public UnableToParseException(String packetType, String message) {
        super(message);
        this.packetType = packetType;
    }

    public String getPacketType() {
        return this.packetType;
    }
}
