/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.StreamHeader;

public class StreamHeaderTxDescriptor
extends StreamHeader {
    private long txDescriptor;
    private int outstandingRequestCount;

    @Override
    public int readFromBytes(byte[] bytes, int offset, int numBytes) {
        int idx = offset;
        idx += super.readFromBytes(bytes, offset, numBytes);
        if (this.length != 18) {
            throw new ServerException("db.mssql.protocol.StreamHeaderWrongSize", this.getTypeName(), 18, this.length);
        }
        this.txDescriptor = DataTypeReader.readEightByteIntegerLow(bytes, idx);
        this.outstandingRequestCount = DataTypeReader.readFourByteIntegerLow(bytes, idx += 8);
        return (idx += 4) - offset;
    }

    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        size += 8;
        return size += 4;
    }

    @Override
    public void write(RawPacketWriter writer) {
        super.write(writer);
        writer.writeEightByteIntegerLow(this.txDescriptor);
        writer.writeFourByteIntegerLow(this.outstandingRequestCount);
    }

    @Override
    public short getType() {
        return 2;
    }

    @Override
    public String getTypeName() {
        return "TransactionDescriptor";
    }

    public long getTxDescriptor() {
        return this.txDescriptor;
    }

    public void setTxDescriptor(long txDescriptor) {
        this.txDescriptor = txDescriptor;
    }

    public int getOutstandingRequestCount() {
        return this.outstandingRequestCount;
    }

    public void setOutstandingRequestCount(int outstandingRequestCount) {
        this.outstandingRequestCount = outstandingRequestCount;
    }
}
