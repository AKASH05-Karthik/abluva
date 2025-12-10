/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.StreamHeader;
import java.util.UUID;

public class StreamHeaderTraceActivity
extends StreamHeader {
    private UUID activityId;
    private int activitySequence;

    @Override
    public int readFromBytes(byte[] bytes, int offset, int numBytes) {
        int idx = offset;
        long highBites = DataTypeReader.readEightByteIntegerLow(bytes, idx);
        long lowBits = DataTypeReader.readEightByteIntegerLow(bytes, idx += 8);
        this.activityId = new UUID(highBites, lowBits);
        this.activitySequence = DataTypeReader.readFourByteIntegerLow(bytes, idx += 8);
        return (idx += 4) - offset;
    }

    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        size += 16;
        return size += 4;
    }

    @Override
    public void write(RawPacketWriter writer) {
        super.write(writer);
        writer.writeEightByteIntegerLow(this.activityId.getMostSignificantBits());
        writer.writeEightByteIntegerLow(this.activityId.getLeastSignificantBits());
        writer.writeFourByteIntegerLow(this.activitySequence);
    }

    @Override
    public short getType() {
        return 3;
    }

    @Override
    public String getTypeName() {
        return "TraceActivity";
    }
}
