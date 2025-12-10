/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.primitives.Ints
 *  com.google.common.primitives.Longs
 *  com.google.common.primitives.Shorts
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.ConnectionState;
import com.galliumdata.server.handler.mssql.PacketReader;
import com.galliumdata.server.handler.mssql.RawPacket;
import com.galliumdata.server.handler.mssql.SMPPacket;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.common.primitives.Shorts;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;

public class RawPacketReader {
    private final ConnectionState connectionState;
    private final PacketReader reader;
    private RawPacket curPkt;
    private int marker;

    public RawPacketReader(ConnectionState connectionState, PacketReader reader) {
        this.connectionState = connectionState;
        this.reader = reader;
    }

    public boolean isDone() {
        if (this.curPkt == null) {
            return false;
        }
        return this.curPkt.isEndOfMessage() && this.curPkt.getNumReadableBytes() == 0;
    }

    public int getNumUnreadBytes() {
        return this.curPkt.getNumReadableBytes();
    }

    public void resetReadIndex() {
        if (this.curPkt == null) {
            this.readNextPacket();
        }
        this.curPkt.setReadIndex(0);
    }

    public void setReadIndex(int idx) {
        this.curPkt.setReadIndex(idx);
    }

    public void resetMarker() {
        this.marker = 0;
    }

    public int getMarker() {
        return this.marker;
    }

    public RawPacket getCurrentPacket() {
        return this.curPkt;
    }

    public void setCurrentPacket(RawPacket pkt, int readIdx) {
        this.curPkt = pkt;
        this.curPkt.setReadIndex(readIdx);
    }

    public void readNextPacket() {
        if (this.curPkt != null && this.curPkt.isEndOfMessage()) {
            throw new ServerException("db.mssql.protocol.NoMoreDataToRead", new Object[0]);
        }
        this.curPkt = this.reader.readNextPacket();
        if (this.curPkt == null) {
            throw new ServerException("db.mssql.protocol.ReadTerminated", new Object[0]);
        }
        if (this.curPkt.buffer[0] == 23) {
            SSLEngineResult sslResult;
            ByteBuffer encryptedBuffer = ByteBuffer.wrap(this.curPkt.buffer);
            byte[] decryptedBytes = new byte[(int)((double)this.connectionState.getPacketSize() * 1.2)];
            ByteBuffer decryptBuffer = ByteBuffer.wrap(decryptedBytes);
            try {
                SSLEngine sslEngine = this.reader.getSslEngine();
                sslResult = sslEngine.unwrap(encryptedBuffer, decryptBuffer);
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            byte[] finalBytes = new byte[sslResult.bytesProduced()];
            System.arraycopy(decryptedBytes, 0, finalBytes, 0, sslResult.bytesProduced());
            this.curPkt = new RawPacket(finalBytes);
        }
        if (this.curPkt.buffer[0] == 83) {
            SMPPacket smpPacket = new SMPPacket(this.connectionState);
            smpPacket.readFromBytes(this.curPkt.buffer, 0, this.curPkt.buffer.length);
            if (smpPacket.getSMPPacketType() != SMPPacket.SMPPacketType.DATA) {
                throw new ServerException("db.mssql.protocol.UnexpectedSMPPacketType", new Object[]{smpPacket.getSMPPacketType()});
            }
            this.curPkt = new RawPacket(smpPacket.getPayload());
        }
        this.curPkt.setReadIndex(8);
    }

    public byte[] readBytes(int numBytes) {
        if (numBytes == 0) {
            return new byte[0];
        }
        if (numBytes < 0) {
            throw new RuntimeException("Unexpected internal error: negative size for raw packet readBytes");
        }
        byte[] buffer = new byte[numBytes];
        int bufferIdx = 0;
        if (this.curPkt.getNumReadableBytes() >= numBytes) {
            this.curPkt.readBytes(buffer, 0, numBytes);
            this.marker += numBytes;
            return buffer;
        }
        int numToRead = this.curPkt.getNumReadableBytes() - bufferIdx;
        int numRead = bufferIdx;
        while (numRead < numBytes - bufferIdx) {
            this.curPkt.readBytes(buffer, numRead, numToRead);
            this.marker += numToRead;
            if (this.curPkt.getNumReadableBytes() == 0 && (numRead += numToRead) < numBytes - bufferIdx) {
                this.readNextPacket();
            }
            if ((numToRead = Math.min(this.curPkt.getNumReadableBytes(), numBytes - numRead)) != 0) continue;
            break;
        }
        return buffer;
    }

    public byte readByte() {
        if (this.curPkt == null || this.curPkt.getNumReadableBytes() < 1) {
            this.readNextPacket();
        }
        ++this.marker;
        return this.curPkt.readByte();
    }

    public short readTwoByteInt() {
        byte[] buffer = this.readBytes(2);
        return Shorts.fromBytes((byte)buffer[0], (byte)buffer[1]);
    }

    public short readTwoByteIntLow() {
        byte[] buffer = this.readBytes(2);
        return Shorts.fromBytes((byte)buffer[1], (byte)buffer[0]);
    }

    public int readFourByteInt() {
        byte[] bytes = this.readBytes(4);
        return Ints.fromBytes((byte)bytes[0], (byte)bytes[1], (byte)bytes[2], (byte)bytes[3]);
    }

    public int readFourByteIntLow() {
        byte[] buffer = this.readBytes(4);
        return Ints.fromBytes((byte)buffer[3], (byte)buffer[2], (byte)buffer[1], (byte)buffer[0]);
    }

    public long readUsignedFourByteIntLow() {
        byte[] bytes = this.readBytes(4);
        return Longs.fromBytes((byte)0, (byte)0, (byte)0, (byte)0, (byte)bytes[3], (byte)bytes[2], (byte)bytes[1], (byte)bytes[0]);
    }

    public long readEightByteInt() {
        byte[] bytes = this.readBytes(8);
        return Longs.fromBytes((byte)bytes[0], (byte)bytes[1], (byte)bytes[2], (byte)bytes[3], (byte)bytes[4], (byte)bytes[5], (byte)bytes[6], (byte)bytes[7]);
    }

    public long readEightByteIntLow() {
        byte[] bytes = this.readBytes(8);
        return Longs.fromBytes((byte)bytes[7], (byte)bytes[6], (byte)bytes[5], (byte)bytes[4], (byte)bytes[3], (byte)bytes[2], (byte)bytes[1], (byte)bytes[0]);
    }

    public long readEightByteDecimal() {
        byte[] bytes = this.readBytes(8);
        return Longs.fromBytes((byte)bytes[3], (byte)bytes[2], (byte)bytes[1], (byte)bytes[0], (byte)bytes[7], (byte)bytes[6], (byte)bytes[5], (byte)bytes[4]);
    }

    public String readString(int numBytes) {
        if (numBytes == 0) {
            return "";
        }
        byte[] bytes = this.readBytes(numBytes);
        return new String(bytes, 0, numBytes, StandardCharsets.UTF_16LE);
    }

    public String readAsciiString(int numBytes) {
        if (numBytes == 0) {
            return "";
        }
        byte[] bytes = this.readBytes(numBytes);
        return new String(bytes, 0, numBytes, StandardCharsets.US_ASCII);
    }

    public String readStringWithEncoding(int numBytes, int collation) {
        Charset charset;
        if (numBytes == 0) {
            return "";
        }
        switch (collation) {
            case 1033: {
                charset = StandardCharsets.ISO_8859_1;
                break;
            }
            default: {
                charset = StandardCharsets.ISO_8859_1;
            }
        }
        byte[] bytes = this.readBytes(numBytes);
        return new String(bytes, 0, numBytes, charset);
    }

    public ByteArray readVarBytes() {
        ByteArray result = new ByteArray();
        result.size = (int)this.readEightByteIntLow();
        if (result.size == -1) {
            return null;
        }
        if (result.size > 500000000 || result.size < 0) {
            throw new ServerException("db.mssql.protocol.ValueOutOfRange", "readVarBytes too large or negative: " + result.size);
        }
        result.bytes = new byte[result.size];
        if (this.curPkt.getNumReadableBytes() > 0 && this.curPkt.getNumReadableBytes() < 4) {
            throw new ServerException("db.mssql.protocol.BadVarLength", "chunk size is broken up");
        }
        int chunkSize = this.readFourByteIntLow();
        int numRead = 0;
        while (chunkSize != 0) {
            System.arraycopy(this.curPkt.getBuffer(), this.curPkt.getReadIndex(), result.bytes, numRead, chunkSize);
            this.curPkt.increaseReadIndex(chunkSize);
            chunkSize = this.readFourByteIntLow();
            if ((numRead += chunkSize) != result.size || chunkSize == 0) continue;
            throw new ServerException("db.mssql.protocol.BadVarLength", "no terminator");
        }
        this.marker += numRead;
        return result;
    }

    public static class ByteArray {
        public int size;
        public byte[] bytes;
    }
}
