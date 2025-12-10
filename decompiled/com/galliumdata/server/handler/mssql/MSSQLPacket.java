/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 *  org.graalvm.polyglot.Value
 *  org.graalvm.polyglot.proxy.ProxyObject
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.GenericPacket;
import com.galliumdata.server.handler.ProtocolException;
import com.galliumdata.server.handler.mssql.ConnectionState;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.DataTypeWriter;
import com.galliumdata.server.handler.mssql.RawPacket;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

public abstract class MSSQLPacket
implements GenericPacket,
ProxyObject {
    protected ConnectionState connectionState;
    protected byte typeCode;
    private boolean statusEndOfMessage;
    private boolean statusIgnoreThisEvent;
    private boolean statusEventNotification;
    private boolean statusResetConnection;
    private boolean statusResetConnectionSkipTran;
    private boolean status5;
    private boolean status6;
    private boolean status7;
    protected int length;
    protected int totalLength;
    private short spid;
    private byte packetId;
    private byte window;
    private boolean removed;
    private boolean modified;
    protected int smpSessionId = -1;
    protected static final Logger log = LogManager.getLogger((String)"galliumdata.dbproto");

    public MSSQLPacket(ConnectionState connState) {
        this.connectionState = connState;
    }

    public int readFromBytes(byte[] bytes, int offset, int numBytes) {
        if (null == bytes) {
            throw new ProtocolException("db.mssql.protocol.ProtocolViolation", "no bytes provided for packet of type " + this.getPacketType() + " (from MSSQLPacket)");
        }
        int idx = offset;
        this.typeCode = DataTypeReader.readByte(bytes, idx);
        byte status = DataTypeReader.readByte(bytes, ++idx);
        ++idx;
        this.statusEndOfMessage = (status & 1) != 0;
        this.statusIgnoreThisEvent = (status & 2) != 0;
        this.statusEventNotification = (status & 4) != 0;
        this.statusResetConnection = (status & 8) != 0;
        this.statusResetConnectionSkipTran = (status & 0x10) != 0;
        this.status5 = (status & 0x20) != 0;
        this.status6 = (status & 0x40) != 0;
        boolean bl = this.status7 = (status & 0x80) != 0;
        if (this.totalLength == 0) {
            this.length = DataTypeReader.readTwoByteInteger(bytes, idx);
            if (this.typeCode == 1 && numBytes > this.length) {
                this.length = numBytes;
            }
        } else {
            this.length = this.totalLength;
        }
        this.spid = DataTypeReader.readTwoByteInteger(bytes, idx += 2);
        this.packetId = DataTypeReader.readByte(bytes, idx += 2);
        this.window = DataTypeReader.readByte(bytes, ++idx);
        return ++idx - offset;
    }

    public void read(RawPacketReader reader) {
        if (reader.getCurrentPacket() != null && reader.getCurrentPacket().isWrappedInSMP()) {
            this.setSMPSessionId(reader.getCurrentPacket().getSMPSessionId());
        }
        this.typeCode = reader.readByte();
        byte status = reader.readByte();
        this.statusEndOfMessage = (status & 1) != 0;
        this.statusIgnoreThisEvent = (status & 2) != 0;
        this.statusEventNotification = (status & 4) != 0;
        this.statusResetConnection = (status & 8) != 0;
        this.statusResetConnectionSkipTran = (status & 0x10) != 0;
        this.status5 = (status & 0x20) != 0;
        this.status6 = (status & 0x40) != 0;
        this.status7 = (status & 0x80) != 0;
        this.length = reader.readTwoByteInt();
        this.spid = reader.readTwoByteInt();
        this.packetId = reader.readByte();
        this.window = reader.readByte();
    }

    public int readFromRawPacket(RawPacket rawPkt) {
        return this.readFromBytes(rawPkt.getBuffer(), 0, rawPkt.getBuffer().length);
    }

    public int getSerializedSize() {
        return 8;
    }

    public final int writeHeaderToBytes(byte[] buffer, int offset) {
        int idx = offset;
        buffer[idx] = this.typeCode;
        buffer[++idx] = this.getStatus();
        DataTypeWriter.encodeTwoByteInteger(buffer, ++idx, (short)this.getSerializedSize());
        DataTypeWriter.encodeTwoByteInteger(buffer, idx += 2, this.spid);
        buffer[idx += 2] = this.packetId;
        buffer[++idx] = this.window;
        return ++idx - offset;
    }

    public byte getStatus() {
        byte status = 0;
        if (this.statusEndOfMessage) {
            status = (byte)(status | 1);
        }
        if (this.statusIgnoreThisEvent) {
            status = (byte)(status | 2);
        }
        if (this.statusEventNotification) {
            status = (byte)(status | 4);
        }
        if (this.statusResetConnection) {
            status = (byte)(status | 8);
        }
        if (this.statusResetConnectionSkipTran) {
            status = (byte)(status | 0x10);
        }
        if (this.status5) {
            status = (byte)(status | 0x20);
        }
        if (this.status6) {
            status = (byte)(status | 0x40);
        }
        if (this.status7) {
            status = (byte)(status | 0x80);
        }
        return status;
    }

    public void write(RawPacketWriter writer) {
        writer.writeByte(this.typeCode);
        writer.writeByte(this.getStatus());
        writer.writeTwoByteIntegerLow(0);
        writer.writeTwoByteInteger(this.spid);
        writer.writeByte((byte)0);
        writer.writeByte(this.window);
    }

    @Override
    public void remove() {
        this.removed = true;
    }

    @Override
    public boolean isRemoved() {
        return this.removed;
    }

    public void setModified() {
        this.modified = true;
    }

    @Override
    public boolean isModified() {
        return this.modified;
    }

    public void copyHeadersFrom(MSSQLPacket otherPacket) {
    }

    public String toString() {
        return "Packet " + this.getPacketType();
    }

    public String toLongString() {
        return this.toString();
    }

    public boolean isStatusEndOfMessage() {
        return this.statusEndOfMessage;
    }

    public void setStatusEndOfMessage(boolean statusEndOfMessage) {
        this.statusEndOfMessage = statusEndOfMessage;
    }

    public boolean isStatusIgnoreThisEvent() {
        return this.statusIgnoreThisEvent;
    }

    public void setStatusIgnoreThisEvent(boolean statusIgnoreThisEvent) {
        this.statusIgnoreThisEvent = statusIgnoreThisEvent;
    }

    public boolean isStatusEventNotification() {
        return this.statusEventNotification;
    }

    public void setStatusEventNotification(boolean statusEventNotification) {
        this.statusEventNotification = statusEventNotification;
    }

    public boolean isStatusResetConnection() {
        return this.statusResetConnection;
    }

    public void setStatusResetConnection(boolean statusResetConnection) {
        this.statusResetConnection = statusResetConnection;
    }

    public boolean isStatusResetConnectionSkipTran() {
        return this.statusResetConnectionSkipTran;
    }

    public void setStatusResetConnectionSkipTran(boolean statusResetConnectionSkipTran) {
        this.statusResetConnectionSkipTran = statusResetConnectionSkipTran;
    }

    public short getSpid() {
        return this.spid;
    }

    public void setSpid(short spid) {
        this.spid = spid;
        this.setModified();
    }

    public byte getPacketId() {
        return this.packetId;
    }

    public void setPacketId(byte packetId) {
        this.packetId = packetId;
        this.setModified();
    }

    public byte getTypeCode() {
        return this.typeCode;
    }

    public void setTypeCode(byte code) {
        this.typeCode = code;
        this.setModified();
    }

    public byte getWindow() {
        return this.window;
    }

    public void setWindow(byte window) {
        this.window = window;
        this.setModified();
    }

    public boolean isWrappedInSMP() {
        return this.smpSessionId != -1;
    }

    public int getSMPSessionId() {
        return this.smpSessionId;
    }

    public void setSMPSessionId(int id) {
        this.smpSessionId = id;
    }

    public Object getMember(String key) {
        switch (key) {
            case "typeCode": {
                return this.typeCode;
            }
            case "packetType": {
                return this.getPacketType();
            }
            case "statusEndOfMessage": {
                return this.statusEndOfMessage;
            }
            case "statusIgnoreThisEvent": {
                return this.statusIgnoreThisEvent;
            }
            case "statusEventNotification": {
                return this.statusEventNotification;
            }
            case "statusResetConnection": {
                return this.statusResetConnection;
            }
            case "statusResetConnectionSkipTran": {
                return this.statusResetConnectionSkipTran;
            }
            case "spid": {
                return this.spid;
            }
            case "packetId": {
                return this.packetId;
            }
            case "window": {
                return this.window;
            }
            case "remove": {
                return arguments -> {
                    this.remove();
                    return null;
                };
            }
            case "toString": {
                return arguments -> this.toString();
            }
        }
        throw new ServerException("db.mssql.logic.NoSuchMember", key);
    }

    public Object getMemberKeys() {
        return new String[]{"typeCode", "packetType", "statusEndOfMessage", "statusIgnoreThisEvent", "statusEventNotification", "statusResetConnection", "statusResetConnectionSkipTran", "spid", "packetId", "window", "remove", "toString"};
    }

    public boolean hasMember(String key) {
        switch (key) {
            case "typeCode": 
            case "packetType": 
            case "statusEndOfMessage": 
            case "statusIgnoreThisEvent": 
            case "statusEventNotification": 
            case "statusResetConnection": 
            case "statusResetConnectionSkipTran": 
            case "spid": 
            case "packetId": 
            case "window": 
            case "remove": 
            case "toString": {
                return true;
            }
        }
        return false;
    }

    public void putMember(String key, Value value) {
        switch (key) {
            case "statusEndOfMessage": {
                this.setStatusEndOfMessage(value.asBoolean());
                break;
            }
            case "statusIgnoreThisEvent": {
                this.setStatusIgnoreThisEvent(value.asBoolean());
                break;
            }
            case "statusResetConnection": {
                this.setStatusResetConnection(value.asBoolean());
                break;
            }
            case "statusEventNotification": {
                this.setStatusEventNotification(value.asBoolean());
                break;
            }
            case "statusResetConnectionSkipTran": {
                this.setStatusResetConnectionSkipTran(value.asBoolean());
                break;
            }
            case "spid": {
                this.setSpid(value.asShort());
                break;
            }
            case "packetId": {
                this.setPacketId(value.asByte());
                break;
            }
            case "window": {
                this.setWindow(value.asByte());
                break;
            }
            default: {
                throw new ServerException("db.mssql.logic.NoSuchMember", key);
            }
        }
    }

    public boolean removeMember(String key) {
        throw new ServerException("db.mssql.logic.CannotRemoveMember", key, this.getPacketType() + " packet");
    }
}
