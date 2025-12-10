/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.DataStreamHeader;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.RawPacketWriter;

public class DataStreamHeaderTraceActivity
extends DataStreamHeader {
    private byte[] activityId = new byte[16];
    private long activitySequence;

    @Override
    public int readFromBytes(byte[] bytes, int offset) {
        int idx = offset;
        int hdrSize = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        if (hdrSize != 24) {
            throw new ServerException("db.mssql.protocol.ErrorInMessageStream", "TransactionDescriptor header size != 0x18");
        }
        short hdrType = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        if (hdrType != 3) {
            throw new ServerException("db.mssql.protocol.ErrorInMessageStream", "TraceActivity type != 3");
        }
        System.arraycopy(bytes, idx, this.activityId, 0, 16);
        this.activitySequence = DataTypeReader.readEightByteIntegerLow(bytes, idx += 16);
        if ((idx += 8) - offset != hdrSize) {
            throw new ServerException("db.mssql.protocol.ErrorInMessageStream", "wrong header size for TraceActivity: " + (idx - offset) + ", expected " + hdrSize);
        }
        return idx - offset;
    }

    @Override
    public int getSerializedSize() {
        return 30;
    }

    @Override
    public void write(RawPacketWriter writer) {
        writer.writeFourByteIntegerLow(24);
        writer.writeTwoByteIntegerLow(3);
        writer.writeBytes(this.activityId, 0, 16);
        writer.writeEightByteIntegerLow(this.activitySequence);
    }

    @Override
    public String getHeaderType() {
        return "TraceActivity";
    }

    public byte[] getActivityId() {
        return this.activityId;
    }

    public void setActivityId(byte[] activityId) {
        this.activityId = activityId;
    }

    public long getActivitySequence() {
        return this.activitySequence;
    }

    public void setActivitySequence(long activitySequence) {
        this.activitySequence = activitySequence;
    }
}
