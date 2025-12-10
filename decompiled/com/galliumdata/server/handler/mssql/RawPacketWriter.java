/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.handler.mssql.ConnectionState;
import com.galliumdata.server.handler.mssql.DataTypeWriter;
import com.galliumdata.server.handler.mssql.MSSQLForwarder;
import com.galliumdata.server.handler.mssql.MSSQLPacket;
import com.galliumdata.server.handler.mssql.RawPacket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RawPacketWriter {
    private final ConnectionState connectionState;
    private final MSSQLForwarder forwarder;
    private final byte modelTypeCode;
    private final byte modelStatus;
    private final short modelSpid;
    private final byte modelPacketId;
    private final byte modelWindow;
    protected RawPacket currentPacket;
    private boolean closed;
    protected byte packetId;
    protected int smpSessionId = -1;
    private boolean inDebugMode;
    private final List<RawPacket> debugPackets = new ArrayList<RawPacket>();

    public RawPacketWriter(ConnectionState connectionState, MSSQLPacket model, MSSQLForwarder forwarder) {
        this.connectionState = connectionState;
        this.forwarder = forwarder;
        if (model.isWrappedInSMP()) {
            this.smpSessionId = model.getSMPSessionId();
        }
        this.modelTypeCode = model.getTypeCode();
        this.modelStatus = model.getStatus();
        this.modelSpid = model.getSpid();
        this.modelPacketId = model.getPacketId();
        this.modelWindow = model.getWindow();
        this.packetId = model.getPacketId();
    }

    public RawPacketWriter(ConnectionState connectionState, RawPacket model, MSSQLForwarder forwarder) {
        this.connectionState = connectionState;
        this.forwarder = forwarder;
        if (model.isWrappedInSMP()) {
            this.smpSessionId = model.getSMPSessionId();
        }
        this.modelTypeCode = model.getTypeCode();
        this.modelStatus = model.getStatus();
        this.modelSpid = model.getSpid();
        this.modelPacketId = model.getPacketId();
        this.modelWindow = model.getWindow();
        this.packetId = model.getPacketId();
    }

    public RawPacket getPacket() {
        if (this.currentPacket == null) {
            this.addPacket();
        }
        if (this.currentPacket.getRemainingBytesToWrite() == 0 && !this.currentPacket.isFinalized()) {
            this.addPacket();
        }
        return this.currentPacket;
    }

    public void addPacket() {
        if (this.currentPacket != null) {
            this.finalizePacket();
            this.currentPacket.setEndOfMessage(false);
            this.sendPacket();
        }
        byte[] buffer = new byte[this.connectionState.getPacketSize()];
        this.currentPacket = new RawPacket(buffer);
        this.writeModelHeader();
    }

    public void close() {
        if (this.closed) {
            return;
        }
        if (this.currentPacket != null) {
            this.finalizePacket();
            this.currentPacket.setEndOfMessage(true);
            this.sendPacket();
            this.closed = true;
        }
    }

    public void finalizePacket() {
        this.currentPacket.finalizeLength();
        this.currentPacket.setPacketId(this.packetId);
        this.packetId = (byte)(this.packetId + 1);
    }

    public void setDebug() {
        this.inDebugMode = true;
    }

    public List<RawPacket> getDebugPackets() {
        if (!this.inDebugMode) {
            throw new RuntimeException("Writer is not in debug mode");
        }
        return this.debugPackets;
    }

    protected void sendPacket() {
        byte[] bytes = this.currentPacket.getWrittenBuffer();
        if (this.inDebugMode) {
            this.debugPackets.add(this.currentPacket);
        }
        try {
            if (this.smpSessionId != -1) {
                this.forwarder.writeOutWithSMP(bytes, 0, bytes.length, this.smpSessionId);
            } else {
                this.forwarder.writeOut(bytes, 0, bytes.length);
            }
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void writeModelHeader() {
        byte[] buffer = this.currentPacket.getBuffer();
        buffer[0] = this.modelTypeCode;
        buffer[1] = this.modelStatus;
        DataTypeWriter.encodeTwoByteInteger(buffer, 4, this.modelSpid);
        buffer[6] = this.modelPacketId;
        buffer[7] = this.modelWindow;
        this.currentPacket.setWriteIndex(8);
    }

    public int getPacketSize() {
        return this.connectionState.getPacketSize();
    }

    public void writeBytes(byte[] bytes, int idx, int numBytes) {
        int numToWrite;
        RawPacket pkt = this.getPacket();
        if (numBytes <= pkt.getRemainingBytesToWrite()) {
            pkt.writeBytes(bytes, idx, numBytes);
            return;
        }
        for (int numWritten = 0; numWritten < numBytes; numWritten += numToWrite) {
            pkt = this.getPacket();
            numToWrite = pkt.getRemainingBytesToWrite();
            if (numBytes - numWritten < numToWrite) {
                numToWrite = numBytes - numWritten;
            }
            pkt.writeBytes(bytes, idx + numWritten, numToWrite);
        }
    }

    public int writeBytesUpToSplit(byte[] bytes, int idx, int numBytes) {
        RawPacket pkt = this.getPacket();
        if (numBytes <= pkt.getRemainingBytesToWrite()) {
            pkt.writeBytes(bytes, idx, numBytes);
            return numBytes;
        }
        int numToWrite = pkt.getRemainingBytesToWrite();
        pkt.writeBytes(bytes, idx, numToWrite);
        return numToWrite;
    }

    public void writeByte(byte b) {
        this.writeBytes(new byte[]{b}, 0, 1);
    }

    public void writeTwoByteInteger(int i) {
        byte[] bytes = new byte[]{(byte)(i >> 8 & 0xFF), (byte)(i & 0xFF)};
        this.writeBytes(bytes, 0, 2);
    }

    public void writeTwoByteIntegerLow(int i) {
        byte[] bytes = new byte[]{(byte)(i & 0xFF), (byte)(i >> 8 & 0xFF)};
        this.writeBytes(bytes, 0, 2);
    }

    public void writeFourByteInteger(int i) {
        byte[] bytes = new byte[]{(byte)(i >> 24 & 0xFF), (byte)(i >> 16 & 0xFF), (byte)(i >> 8 & 0xFF), (byte)(i & 0xFF)};
        this.writeBytes(bytes, 0, 4);
    }

    public void writeFourByteIntegerLow(int i) {
        byte[] bytes = new byte[]{(byte)(i & 0xFF), (byte)(i >> 8 & 0xFF), (byte)(i >> 16 & 0xFF), (byte)(i >> 24 & 0xFF)};
        this.writeBytes(bytes, 0, 4);
    }

    public void writeFourByteIntegerLowNoBreak(int i) {
        if (this.getPacket().getRemainingBytesToWrite() < 4) {
            this.addPacket();
        }
        byte[] bytes = new byte[]{(byte)(i & 0xFF), (byte)(i >> 8 & 0xFF), (byte)(i >> 16 & 0xFF), (byte)(i >> 24 & 0xFF)};
        this.writeBytes(bytes, 0, 4);
    }

    public void writeEightByteIntegerLow(long i) {
        byte[] bytes = new byte[]{(byte)(i & 0xFFL), (byte)(i >> 8 & 0xFFL), (byte)(i >> 16 & 0xFFL), (byte)(i >> 24 & 0xFFL), (byte)(i >> 32 & 0xFFL), (byte)(i >> 40 & 0xFFL), (byte)(i >> 48 & 0xFFL), (byte)(i >> 56 & 0xFFL)};
        this.writeBytes(bytes, 0, 8);
    }

    public void writeEightByteNumber(long num) {
        byte[] buffer = new byte[]{(byte)(num >> 0 & 0xFFL), (byte)(num >> 8 & 0xFFL), (byte)(num >> 16 & 0xFFL), (byte)(num >> 24 & 0xFFL), (byte)(num >> 32 & 0xFFL), (byte)(num >> 40 & 0xFFL), (byte)(num >> 48 & 0xFFL), (byte)(num >> 56 & 0xFFL)};
        this.writeBytes(buffer, 0, 8);
    }

    public void writeEightByteDecimal(long num) {
        byte[] buffer = new byte[]{(byte)(num >> 32 & 0xFFL), (byte)(num >> 40 & 0xFFL), (byte)(num >> 48 & 0xFFL), (byte)(num >> 56 & 0xFFL), (byte)(num >> 0 & 0xFFL), (byte)(num >> 8 & 0xFFL), (byte)(num >> 16 & 0xFFL), (byte)(num >> 24 & 0xFFL)};
        this.writeBytes(buffer, 0, 8);
    }

    public void writeStringWithEncoding(String s, int collation) {
        Charset charset;
        switch (collation) {
            case 1033: {
                charset = StandardCharsets.US_ASCII;
                break;
            }
            default: {
                charset = StandardCharsets.ISO_8859_1;
            }
        }
        byte[] bytes = s.getBytes(charset);
        this.writeBytes(bytes, 0, bytes.length);
    }
}
