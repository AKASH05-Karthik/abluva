/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.handler.mssql.ConnectionState;
import com.galliumdata.server.handler.mssql.PacketReader;
import com.galliumdata.server.handler.mssql.RawPacket;

public class SimplePacketReader
extends PacketReader {
    private RawPacket rawPkt;

    public SimplePacketReader(ConnectionState connState, RawPacket pkt) {
        super(null, null, null);
        this.rawPkt = pkt;
    }

    @Override
    public RawPacket readNextPacket() {
        if (this.rawPkt == null) {
            return null;
        }
        RawPacket pkt = this.rawPkt;
        this.rawPkt = null;
        return pkt;
    }

    @Override
    public void close() {
        this.rawPkt = null;
    }
}
