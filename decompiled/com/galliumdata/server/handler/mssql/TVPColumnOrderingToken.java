/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import java.util.ArrayList;
import java.util.List;

public class TVPColumnOrderingToken {
    private List<Integer> colNums = new ArrayList<Integer>();

    public int readFromBytes(byte[] bytes, int offset) {
        int idx = offset;
        int totalCount = DataTypeReader.readTwoByteIntegerLow(bytes, offset);
        idx += 2;
        for (int i = 0; i < totalCount; ++i) {
            short colNum = DataTypeReader.readTwoByteIntegerLow(bytes, offset);
            idx += 2;
            this.colNums.add(Integer.valueOf(colNum));
        }
        return idx - offset;
    }

    public void write(RawPacketWriter writer) {
        writer.writeTwoByteIntegerLow(this.colNums.size());
        for (Integer colNum : this.colNums) {
            writer.writeTwoByteIntegerLow(colNum);
        }
    }
}
