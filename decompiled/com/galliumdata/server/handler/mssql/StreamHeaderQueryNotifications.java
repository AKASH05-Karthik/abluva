/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.StreamHeader;
import java.nio.charset.StandardCharsets;

public class StreamHeaderQueryNotifications
extends StreamHeader {
    private String notifyId;
    private String ssbDeployment;
    private int notifyTimeout;

    @Override
    public int readFromBytes(byte[] bytes, int offset, int numBytes) {
        int idx = offset;
        idx += super.readFromBytes(bytes, idx, numBytes);
        short len = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        this.notifyId = new String(bytes, idx += 2, len * 2, StandardCharsets.UTF_16LE);
        idx += len * 2;
        len = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        this.ssbDeployment = new String(bytes, idx += 2, len * 2, StandardCharsets.UTF_16LE);
        int sizeSoFar = (idx += len * 2) - offset + 5;
        if (this.length == sizeSoFar) {
            return idx - offset;
        }
        if (this.length != sizeSoFar + 4) {
            throw new ServerException("db.mssql.protocol.StreamHeaderError", this.getTypeName(), "notifyTimeout: wrong size: " + (idx - offset + 5));
        }
        this.notifyTimeout = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        return (idx += 4) - offset;
    }

    @Override
    public int getSerializedSize() {
        int size = 0;
        return size;
    }

    @Override
    public void write(RawPacketWriter writer) {
        super.write(writer);
    }

    @Override
    public short getType() {
        return 1;
    }

    @Override
    public String getTypeName() {
        return "QueryNotification";
    }

    public String getNotifyId() {
        return this.notifyId;
    }

    public void setNotifyId(String notifyId) {
        this.notifyId = notifyId;
    }

    public String getSsbDeployment() {
        return this.ssbDeployment;
    }

    public void setSsbDeployment(String ssbDeployment) {
        this.ssbDeployment = ssbDeployment;
    }

    public int getNotifyTimeout() {
        return this.notifyTimeout;
    }

    public void setNotifyTimeout(int notifyTimeout) {
        this.notifyTimeout = notifyTimeout;
    }
}
