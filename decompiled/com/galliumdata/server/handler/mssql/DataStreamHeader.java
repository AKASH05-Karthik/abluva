/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.DataStreamHeaderQueryNotifications;
import com.galliumdata.server.handler.mssql.DataStreamHeaderTraceActivity;
import com.galliumdata.server.handler.mssql.DataStreamHeaderTxDescriptor;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.RawPacketWriter;

public abstract class DataStreamHeader {
    public static DataStreamHeader createDataStreamHeader(byte[] bytes, int offset) {
        short type = DataTypeReader.readTwoByteIntegerLow(bytes, offset + 4);
        switch (type) {
            case 1: {
                return new DataStreamHeaderQueryNotifications();
            }
            case 2: {
                return new DataStreamHeaderTxDescriptor();
            }
            case 3: {
                return new DataStreamHeaderTraceActivity();
            }
        }
        throw new ServerException("db.mssql.protocol.UnknownDataStreamHeader", type);
    }

    public abstract int readFromBytes(byte[] var1, int var2);

    public abstract int getSerializedSize();

    public abstract void write(RawPacketWriter var1);

    public abstract String getHeaderType();
}
