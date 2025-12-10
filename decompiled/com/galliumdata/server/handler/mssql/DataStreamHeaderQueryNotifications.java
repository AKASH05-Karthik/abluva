/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.DataStreamHeader;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import java.nio.charset.StandardCharsets;

public class DataStreamHeaderQueryNotifications
extends DataStreamHeader {
    private String notifyId;
    private String ssbDeployment;
    private Integer notifyTimeout;

    @Override
    public int readFromBytes(byte[] bytes, int offset) {
        int idx = offset;
        int hdrSize = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        short hdrType = DataTypeReader.readTwoByteIntegerLow(bytes, idx += 4);
        idx += 2;
        if (hdrType != 1) {
            throw new ServerException("db.mssql.protocol.ErrorInMessageStream", "QueryNotifications type != 1");
        }
        short len = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        this.notifyId = new String(bytes, idx += 2, (int)len, StandardCharsets.UTF_16LE);
        idx += len;
        len = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        this.ssbDeployment = new String(bytes, idx += 2, (int)len, StandardCharsets.UTF_16LE);
        if (hdrSize == (idx += len) - offset + 4) {
            this.notifyTimeout = DataTypeReader.readFourByteIntegerLow(bytes, idx);
            idx += 4;
        }
        if (idx - offset != hdrSize) {
            throw new ServerException("db.mssql.protocol.ErrorInMessageStream", "wrong header size for QueryNotifications: " + (idx - offset) + ", expected " + hdrSize);
        }
        return hdrSize;
    }

    @Override
    public int getSerializedSize() {
        int size = 0;
        size += 4;
        size += 2;
        size += 2;
        size += this.notifyId.length() * 2;
        size += 2;
        size += this.ssbDeployment.length() * 2;
        if (this.notifyTimeout != null) {
            size += 4;
        }
        return size;
    }

    @Override
    public void write(RawPacketWriter writer) {
        int hdrSize = this.getSerializedSize();
        writer.writeFourByteIntegerLow(hdrSize - 4);
        writer.writeTwoByteIntegerLow(1);
        writer.writeByte((byte)this.notifyId.length());
        byte[] strBytes = this.notifyId.getBytes(StandardCharsets.UTF_16LE);
        writer.writeBytes(strBytes, 0, strBytes.length);
        writer.writeByte((byte)this.ssbDeployment.length());
        strBytes = this.ssbDeployment.getBytes(StandardCharsets.UTF_16LE);
        writer.writeBytes(strBytes, 0, strBytes.length);
        if (this.notifyTimeout != null) {
            writer.writeFourByteIntegerLow(this.notifyTimeout);
        }
    }

    @Override
    public String getHeaderType() {
        return "TransactionDescriptor";
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
