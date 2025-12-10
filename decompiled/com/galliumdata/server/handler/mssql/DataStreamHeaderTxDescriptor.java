/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.DataStreamHeader;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.RawPacketWriter;

public class DataStreamHeaderTxDescriptor
extends DataStreamHeader {
    private long transactionDescription;
    private int outstandingRequestCount;

    @Override
    public int readFromBytes(byte[] bytes, int offset) {
        int idx = offset;
        int hdrSize = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        idx += 4;
        if (hdrSize != 18) {
            throw new ServerException("db.mssql.protocol.ErrorInMessageStream", "TransactionDescriptor header size != 0x12");
        }
        short hdrType = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        if (hdrType != 2) {
            throw new ServerException("db.mssql.protocol.ErrorInMessageStream", "TransactionDescriptor type != 2");
        }
        this.transactionDescription = DataTypeReader.readEightByteIntegerLow(bytes, idx);
        this.outstandingRequestCount = DataTypeReader.readFourByteIntegerLow(bytes, idx += 8);
        if ((idx += 4) - offset != hdrSize) {
            throw new ServerException("db.mssql.protocol.ErrorInMessageStream", "wrong header size for TxDescriptor: " + (idx - offset) + ", expected " + hdrSize);
        }
        return idx - offset;
    }

    @Override
    public int getSerializedSize() {
        return 18;
    }

    @Override
    public void write(RawPacketWriter writer) {
        writer.writeFourByteIntegerLow(18);
        writer.writeTwoByteIntegerLow(2);
        writer.writeEightByteIntegerLow(this.transactionDescription);
        writer.writeFourByteIntegerLow(this.outstandingRequestCount);
    }

    @Override
    public String getHeaderType() {
        return "TransactionDescriptor";
    }

    public long getTransactionDescription() {
        return this.transactionDescription;
    }

    public void setTransactionDescription(long transactionDescription) {
        this.transactionDescription = transactionDescription;
    }

    public int getOutstandingRequestCount() {
        return this.outstandingRequestCount;
    }

    public void setOutstandingRequestCount(int outstandingRequestCount) {
        this.outstandingRequestCount = outstandingRequestCount;
    }
}
