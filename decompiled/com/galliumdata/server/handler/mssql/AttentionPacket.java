/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.handler.mssql.ConnectionState;
import com.galliumdata.server.handler.mssql.MSSQLPacket;
import com.galliumdata.server.handler.mssql.RawPacketWriter;

public class AttentionPacket
extends MSSQLPacket {
    public AttentionPacket(ConnectionState connectionState) {
        super(connectionState);
        this.typeCode = (byte)6;
    }

    @Override
    public int readFromBytes(byte[] bytes, int offset, int numBytes) {
        return super.readFromBytes(bytes, offset, numBytes);
    }

    @Override
    public int getSerializedSize() {
        return super.getSerializedSize();
    }

    @Override
    public void write(RawPacketWriter writer) {
        writer.getPacket().setWriteIndex(8);
    }

    @Override
    public String getPacketType() {
        return "Attention";
    }
}
