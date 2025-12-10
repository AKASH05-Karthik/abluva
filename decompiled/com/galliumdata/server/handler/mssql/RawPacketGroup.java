/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.handler.mssql.MSSQLPacket;
import com.galliumdata.server.handler.mssql.RawPacket;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class RawPacketGroup {
    private final List<RawPacket> packets = new Vector<RawPacket>();

    public RawPacketGroup() {
    }

    public RawPacketGroup(RawPacket pkt) {
        this.packets.add(pkt);
    }

    public RawPacketGroup(RawPacketGroup other) {
        this.packets.addAll(other.packets);
    }

    public List<RawPacket> getPackets() {
        return new ArrayList<RawPacket>(this.packets);
    }

    public RawPacket getPacketAt(int idx) {
        return this.packets.get(idx);
    }

    public int getSize() {
        return this.packets.size();
    }

    public void addRawPacket(RawPacket pkt) {
        RawPacket copy = new RawPacket(pkt);
        this.packets.add(copy);
    }

    public void addRawPacketNoCopy(RawPacket pkt) {
        this.packets.add(pkt);
    }

    public void addRawPacketGroup(RawPacketGroup grp) {
        for (int i = 0; i < grp.getSize(); ++i) {
            this.addRawPacket(grp.getPackets().get(i));
        }
    }

    public void addPacket(MSSQLPacket pkt) {
        RawPacketWriter writer = new RawPacketWriter(pkt.connectionState, pkt, null);
        pkt.write(writer);
        writer.finalizePacket();
        this.addRawPacket(writer.getPacket());
    }

    public void clear() {
        this.packets.clear();
    }
}
