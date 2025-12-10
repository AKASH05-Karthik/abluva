/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.StreamHeaderQueryNotifications;
import com.galliumdata.server.handler.mssql.StreamHeaderTraceActivity;
import com.galliumdata.server.handler.mssql.StreamHeaderTxDescriptor;

public abstract class StreamHeader {
    protected int length;

    public int readFromBytes(byte[] bytes, int offset, int numBytes) {
        int idx = offset;
        this.length = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        short type = DataTypeReader.readTwoByteIntegerLow(bytes, idx += 4);
        idx += 2;
        if (type != this.getType()) {
            throw new RuntimeException("Unexpected: StreamHeader has wrong type: " + type + ", expected " + this.getType());
        }
        return idx - offset;
    }

    public int getSerializedSize() {
        int size = 0;
        size += 4;
        return size += 2;
    }

    public void write(RawPacketWriter writer) {
        int len = this.getSerializedSize();
        writer.writeFourByteIntegerLow(len);
        writer.writeTwoByteIntegerLow(this.getType());
    }

    public abstract short getType();

    public abstract String getTypeName();

    public static StreamHeader createStreamHeader(byte type) {
        switch (type) {
            case 1: {
                return new StreamHeaderQueryNotifications();
            }
            case 2: {
                return new StreamHeaderTxDescriptor();
            }
            case 3: {
                return new StreamHeaderTraceActivity();
            }
        }
        throw new ServerException("db.mssql.protocol.StreamHeaderError", type, "unsupported type");
    }
}
