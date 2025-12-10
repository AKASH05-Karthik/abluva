/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.DataTypeWriter;
import java.util.concurrent.atomic.AtomicLong;

public class RawPacket {
    public byte[] buffer;
    public long id = idGen.incrementAndGet();
    private static final AtomicLong idGen = new AtomicLong();
    private final long ts = System.nanoTime();
    private int readIndex = 0;
    private int writeIndex = 0;
    private boolean finalized;
    private int smpSessionId = -1;
    protected int realLength;

    public RawPacket(byte[] buffer) {
        this.buffer = buffer;
    }

    public RawPacket(RawPacket orig) {
        this.buffer = new byte[orig.getBuffer().length];
        this.id = orig.id;
        this.smpSessionId = orig.smpSessionId;
        System.arraycopy(orig.getBuffer(), 0, this.buffer, 0, orig.getBuffer().length);
    }

    public byte[] getBuffer() {
        return this.buffer;
    }

    public byte[] getWrittenBuffer() {
        if (this.writeIndex == this.buffer.length) {
            return this.buffer;
        }
        byte[] buf = new byte[this.writeIndex];
        System.arraycopy(this.buffer, 0, buf, 0, this.writeIndex);
        return buf;
    }

    public void addBytes(byte[] addBytes, int offset) {
        byte[] newBuffer = new byte[this.buffer.length + addBytes.length - offset];
        System.arraycopy(this.buffer, 0, newBuffer, 0, this.buffer.length);
        System.arraycopy(addBytes, offset, newBuffer, this.buffer.length, addBytes.length - offset);
        this.buffer = newBuffer;
    }

    public byte getTypeCode() {
        if (this.buffer == null || this.buffer.length == 0) {
            throw new ServerException("db.mssql.protocol.EmptyPacket", new Object[0]);
        }
        return this.buffer[0];
    }

    public int getReadIndex() {
        return this.readIndex;
    }

    public void setReadIndex(int idx) {
        this.readIndex = idx;
    }

    public void increaseReadIndex(int delta) {
        this.readIndex += delta;
        if (this.readIndex > this.buffer.length) {
            throw new RuntimeException("Internal error: RawPacket readIndex > buffer length");
        }
    }

    public int getWriteIndex() {
        return this.writeIndex;
    }

    public void setWriteIndex(int idx) {
        this.writeIndex = idx;
    }

    public int getNumReadableBytes() {
        return this.buffer.length - this.readIndex;
    }

    public int getRemainingBytesToWrite() {
        return this.buffer.length - this.writeIndex;
    }

    public byte readByte() {
        if (this.getNumReadableBytes() < 1) {
            throw new ServerException("db.mssql.protocol.InternalError", "Underflow in RawPacket.readByte");
        }
        byte b = this.buffer[this.readIndex];
        ++this.readIndex;
        if (this.readIndex > this.buffer.length) {
            throw new RuntimeException("Internal error: RawPacket readIndex > buffer length");
        }
        return b;
    }

    public void readBytes(byte[] buf, int idx, int numBytes) {
        if (this.getNumReadableBytes() < numBytes) {
            throw new ServerException("db.mssql.protocol.InternalError", "Underflow in RawPacket.readBytes");
        }
        if (numBytes < 0) {
            throw new ServerException("db.mssql.protocol.InternalError", "Negative numBytes in RawPacket.readBytes");
        }
        System.arraycopy(this.buffer, this.readIndex, buf, idx, numBytes);
        this.readIndex += numBytes;
        if (this.readIndex > this.buffer.length) {
            throw new RuntimeException("Internal error: RawPacket readIndex > buffer length");
        }
    }

    public void writeBytes(byte[] bytes, int idx, int numBytes) {
        if (numBytes > this.getRemainingBytesToWrite()) {
            throw new ServerException("db.mssql.protocol.InternalError", "Overflow in RawPacket");
        }
        System.arraycopy(bytes, idx, this.buffer, this.writeIndex, numBytes);
        this.writeIndex += numBytes;
    }

    public byte getStatus() {
        return this.buffer[1];
    }

    public void setStatus(byte status) {
        this.buffer[1] = status;
    }

    public boolean isEndOfMessage() {
        return (this.buffer[1] & 1) != 0;
    }

    public void setEndOfMessage(boolean b) {
        this.buffer[1] = b ? (byte)1 : 0;
    }

    public short getSpid() {
        return DataTypeReader.readTwoByteInteger(this.buffer, 4);
    }

    public void setSpid(short spid) {
        DataTypeWriter.encodeTwoByteInteger(this.buffer, 4, spid);
    }

    public byte getPacketId() {
        return this.buffer[6];
    }

    public void setPacketId(int id) {
        this.buffer[6] = (byte)id;
    }

    public byte getWindow() {
        return this.buffer[7];
    }

    public void setWindow(byte window) {
        this.buffer[7] = window;
    }

    public void finalizeLength() {
        if (this.isWrappedInSMP()) {
            DataTypeWriter.encodeFourByteInteger(this.buffer, 4, (short)this.writeIndex);
            DataTypeWriter.encodeTwoByteInteger(this.buffer, 18, (short)(this.writeIndex - 16));
        } else {
            DataTypeWriter.encodeTwoByteInteger(this.buffer, 2, (short)this.writeIndex);
        }
        if (this.buffer.length > this.writeIndex) {
            byte[] newBuffer = new byte[this.writeIndex];
            System.arraycopy(this.buffer, 0, newBuffer, 0, this.writeIndex);
            this.buffer = newBuffer;
        }
        this.finalized = true;
    }

    public boolean isFinalized() {
        return this.finalized;
    }

    public boolean isWrappedInSMP() {
        return this.smpSessionId != -1;
    }

    public int getSMPSessionId() {
        return this.smpSessionId;
    }

    public void setSMPSessionId(int i) {
        this.smpSessionId = i;
    }
}
