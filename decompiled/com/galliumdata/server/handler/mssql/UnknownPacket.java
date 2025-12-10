/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.graalvm.polyglot.Value
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.ConnectionState;
import com.galliumdata.server.handler.mssql.MSSQLPacket;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import java.util.Arrays;
import java.util.List;
import org.graalvm.polyglot.Value;

public class UnknownPacket
extends MSSQLPacket {
    private byte[] data;

    public UnknownPacket(ConnectionState connectionState) {
        super(connectionState);
    }

    @Override
    public int readFromBytes(byte[] bytes, int offset, int numBytes) {
        int idx = offset;
        int dataLen = this.length - 8;
        this.data = new byte[dataLen];
        System.arraycopy(bytes, idx += super.readFromBytes(bytes, offset, numBytes), this.data, 0, dataLen);
        return this.length;
    }

    @Override
    public int getSerializedSize() {
        return 8 + this.data.length;
    }

    @Override
    public void write(RawPacketWriter writer) {
        writer.writeBytes(this.data, 0, this.data.length);
    }

    @Override
    public String getPacketType() {
        return "Unknown";
    }

    @Override
    public String toString() {
        return "Unknown message: " + this.data.length + " bytes";
    }

    public byte[] getData() {
        return this.data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public Object getMember(String key) {
        switch (key) {
            case "data": {
                return this.data;
            }
        }
        return super.getMember(key);
    }

    @Override
    public Object getMemberKeys() {
        String[] parentKeys = (String[])super.getMemberKeys();
        List<String> keys = Arrays.asList(parentKeys);
        keys.add("data");
        return keys.toArray();
    }

    @Override
    public boolean hasMember(String key) {
        switch (key) {
            case "data": {
                return true;
            }
        }
        return super.hasMember(key);
    }

    @Override
    public void putMember(String key, Value value) {
        switch (key) {
            case "data": {
                if (!value.hasArrayElements()) {
                    throw new ServerException("db.mssql.logic.ValueHasWrongType", "byte array", value);
                }
                int arraySize = (int)value.getArraySize();
                byte[] bytes = new byte[arraySize];
                for (int i = 0; i < arraySize; ++i) {
                    Value elem = value.getArrayElement((long)i);
                    if (!elem.fitsInByte()) {
                        throw new ServerException("db.mssql.logic.ValueHasWrongType", "byte", elem);
                    }
                    bytes[i] = elem.asByte();
                }
                this.setData(bytes);
                break;
            }
            default: {
                throw new ServerException("db.mssql.logic.NoSuchMember", key);
            }
        }
    }

    @Override
    public boolean removeMember(String key) {
        throw new ServerException("db.mssql.logic.CannotRemoveMember", key, "Unknown packet");
    }
}
