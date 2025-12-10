/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import java.util.ArrayList;
import java.util.List;

public class TVPOrderUniqueToken {
    private List<OrderUnique> orderUniques = new ArrayList<OrderUnique>();

    public int readFromBytes(byte[] bytes, int offset) {
        int idx = offset;
        int totalCount = DataTypeReader.readTwoByteIntegerLow(bytes, offset);
        idx += 2;
        for (int i = 0; i < totalCount; ++i) {
            OrderUnique orderUnique = new OrderUnique();
            orderUnique.colNum = DataTypeReader.readTwoByteIntegerLow(bytes, offset);
            byte flags = bytes[idx += 2];
            ++idx;
            orderUnique.fOrderAsc = (flags & 1) != 0;
            orderUnique.fOrderDesc = (flags & 2) != 0;
            orderUnique.fUnique = (flags & 4) != 0;
            this.orderUniques.add(orderUnique);
        }
        return idx - offset;
    }

    public void write(RawPacketWriter writer) {
        writer.writeTwoByteIntegerLow(this.orderUniques.size());
        for (OrderUnique ou : this.orderUniques) {
            writer.writeTwoByteIntegerLow(ou.colNum);
            byte flags = 0;
            if (ou.fOrderAsc) {
                flags = (byte)(flags | 1);
            }
            if (ou.fOrderDesc) {
                flags = (byte)(flags | 2);
            }
            if (ou.fUnique) {
                flags = (byte)(flags | 4);
            }
            writer.writeByte(flags);
        }
    }

    public static class OrderUnique {
        public int colNum;
        public boolean fOrderAsc;
        public boolean fOrderDesc;
        public boolean fUnique;
    }
}
