/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.ConnectionState;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.DataTypeWriter;
import com.galliumdata.server.handler.mssql.MSSQLPacket;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.log.Markers;

public class SMPPacket
extends MSSQLPacket {
    private byte flags;
    private int seqNum;
    private int window;
    private byte[] payload;

    public SMPPacket(ConnectionState connectionState) {
        super(connectionState);
        this.typeCode = (byte)83;
    }

    @Override
    public int readFromBytes(byte[] bytes, int offset, int numBytes) {
        byte type = bytes[0];
        if (type != 83) {
            throw new ServerException("db.mssql.protocol.BadSMPSyn", "Unexpected type, expected 0x53, got: " + type);
        }
        this.flags = bytes[1];
        this.smpSessionId = DataTypeReader.readTwoByteIntegerLow(bytes, 2);
        int pktSize = DataTypeReader.readFourByteIntegerLow(bytes, 4);
        this.seqNum = DataTypeReader.readFourByteIntegerLow(bytes, 8);
        this.window = DataTypeReader.readFourByteIntegerLow(bytes, 12);
        if (pktSize > 16) {
            this.payload = new byte[pktSize - 16];
            System.arraycopy(bytes, offset + 16, this.payload, 0, pktSize - 16);
        }
        return pktSize;
    }

    @Override
    public int getSerializedSize() {
        int size = 16;
        if (this.payload != null) {
            size += this.payload.length;
        }
        return size;
    }

    @Override
    public void write(RawPacketWriter writer) {
        throw new RuntimeException("SMPPacket.write is not implemented");
    }

    public byte[] serialize() {
        byte[] bytes = new byte[this.getSerializedSize()];
        int idx = 0;
        bytes[idx] = 83;
        bytes[++idx] = this.flags;
        DataTypeWriter.encodeTwoByteIntegerLow(bytes, ++idx, (short)this.smpSessionId);
        DataTypeWriter.encodeFourByteIntegerLow(bytes, idx += 2, this.getSerializedSize());
        DataTypeWriter.encodeFourByteIntegerLow(bytes, idx += 4, this.seqNum);
        DataTypeWriter.encodeFourByteIntegerLow(bytes, idx += 4, this.window);
        idx += 4;
        if (this.seqNum > this.window && log.isDebugEnabled()) {
            log.debug(Markers.MSSQL, "SMP packet has seqNum " + this.seqNum + " > window " + this.window);
        }
        if (this.payload != null) {
            System.arraycopy(this.payload, 0, bytes, idx, this.payload.length);
            idx += this.payload.length;
        }
        if (idx != bytes.length) {
            throw new RuntimeException("Internal error: SMPPacket.writeToBytes has not filled the buffer");
        }
        return bytes;
    }

    @Override
    public String getPacketType() {
        return "SMP - " + String.valueOf((Object)this.getSMPPacketType());
    }

    @Override
    public String toString() {
        String s = "SMP packet - " + String.valueOf((Object)this.getSMPPacketType());
        if (this.payload != null) {
            s = s + " [" + this.payload.length + " bytes]";
        }
        return s;
    }

    public SMPPacketType getSMPPacketType() {
        switch (this.flags) {
            case 1: {
                return SMPPacketType.SYN;
            }
            case 2: {
                return SMPPacketType.ACK;
            }
            case 4: {
                return SMPPacketType.FIN;
            }
            case 8: {
                return SMPPacketType.DATA;
            }
        }
        throw new ServerException("db.mssql.protocol.BadSMPSyn", "Unknown SMP packet type: " + this.flags);
    }

    public int getSeqNum() {
        return this.seqNum;
    }

    public void setSeqNum(int seqNum) {
        this.seqNum = seqNum;
    }

    public int getSMPWindow() {
        return this.window;
    }

    public void setSMPWindow(int window) {
        this.window = window;
    }

    public byte[] getPayload() {
        return this.payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public static enum SMPPacketType {
        SYN,
        ACK,
        FIN,
        DATA;

    }
}
