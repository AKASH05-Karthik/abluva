/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.handler.mssql.ConnectionState;
import com.galliumdata.server.handler.mssql.DataStreamHeader;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.MSSQLPacket;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import java.util.ArrayList;
import java.util.List;

public abstract class DataStreamPacket
extends MSSQLPacket {
    private List<DataStreamHeader> dataStreamHeaders = new ArrayList<DataStreamHeader>();

    public DataStreamPacket(ConnectionState connectionState) {
        super(connectionState);
    }

    @Override
    public int readFromBytes(byte[] bytes, int offset, int numBytes) {
        int idx = offset;
        idx += super.readFromBytes(bytes, offset, numBytes);
        if (this.connectionState.tdsVersion72andHigher()) {
            int headersSize = DataTypeReader.readFourByteIntegerLow(bytes, idx);
            idx += 4;
            while (idx - offset < headersSize) {
                DataStreamHeader hdr = DataStreamHeader.createDataStreamHeader(bytes, idx);
                idx += hdr.readFromBytes(bytes, idx);
                this.dataStreamHeaders.add(hdr);
            }
        }
        return idx - offset;
    }

    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        if (this.connectionState.tdsVersion72andHigher()) {
            size += 4;
            for (DataStreamHeader hdr : this.dataStreamHeaders) {
                size += hdr.getSerializedSize();
            }
        }
        return size;
    }

    @Override
    public void write(RawPacketWriter writer) {
        if (this.connectionState.tdsVersion72andHigher()) {
            int hdrSize = 0;
            for (DataStreamHeader hdr : this.dataStreamHeaders) {
                hdrSize += hdr.getSerializedSize();
            }
            writer.writeFourByteIntegerLow(hdrSize + 4);
            for (DataStreamHeader hdr : this.dataStreamHeaders) {
                hdr.write(writer);
            }
        }
    }

    @Override
    public String getPacketType() {
        return "DataStream";
    }

    public List<DataStreamHeader> getDataStreamHeaders() {
        return this.dataStreamHeaders;
    }
}
