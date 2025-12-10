/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.handler.mssql.DataTypeWriter;
import com.galliumdata.server.handler.mssql.RawPacket;
import java.util.List;

public class PacketUtil {
    public static byte[] joinPackets(List<RawPacket> pkts) {
        int fullSize = 0;
        for (int i = 0; i < pkts.size(); ++i) {
            RawPacket p = pkts.get(i);
            if (fullSize == 0) {
                fullSize += p.getBuffer().length;
                continue;
            }
            fullSize += p.getBuffer().length - 8;
        }
        byte[] fullBytes = new byte[fullSize];
        int idx = 0;
        for (int i = 0; i < pkts.size(); ++i) {
            byte[] bytes = pkts.get(i).getBuffer();
            if (idx == 0) {
                System.arraycopy(bytes, 0, fullBytes, 0, bytes.length);
                idx += bytes.length;
                continue;
            }
            System.arraycopy(bytes, 8, fullBytes, idx, bytes.length - 8);
            idx += bytes.length - 8;
        }
        DataTypeWriter.encodeTwoByteInteger(fullBytes, 2, (short)fullSize);
        return fullBytes;
    }
}
