/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.TypeInfo;
import java.nio.charset.StandardCharsets;

public class TVPTypeInfoColumnMetadata {
    private int userType;
    private boolean fNullable;
    private boolean fCaseSen;
    private byte usUpdateable;
    private boolean fIdentity;
    private boolean fComputed;
    private byte usReservedODBC;
    private boolean fFixedLenCLRType;
    private boolean fDefault;
    private byte usReserved;
    private TypeInfo typeInfo = new TypeInfo();
    private String columnName;

    public int readFromBytes(byte[] bytes, int offset, int numBytes) {
        int idx = offset;
        this.userType = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        short flags = DataTypeReader.readTwoByteIntegerLow(bytes, idx += 4);
        idx += 2;
        this.fNullable = (flags & 1) > 0;
        this.fCaseSen = (flags & 2) > 0;
        this.usUpdateable = (byte)((flags & 0xC) >> 2);
        this.fIdentity = (flags & 0x10) > 0;
        this.fComputed = (flags & 0x20) > 0;
        this.usReservedODBC = (byte)((flags & 0xC0) >> 6);
        this.fFixedLenCLRType = (flags & 0x100) > 0;
        this.fDefault = (flags & 0x200) > 0;
        this.usReserved = (byte)((flags & 0xFC00) >> 10);
        idx += this.typeInfo.readFromBytes(bytes, idx);
        if (this.typeInfo.getType() == 106 || this.typeInfo.getType() == 108) {
            this.typeInfo.setVariantScale(2);
        }
        byte colNameLen = bytes[idx];
        ++idx;
        if (colNameLen > 0) {
            this.columnName = new String(bytes, idx, (int)colNameLen, StandardCharsets.UTF_16LE);
            idx += colNameLen;
        }
        return idx - offset;
    }

    public int getSerializedSize() {
        int size = 4;
        size += 2;
        size += this.typeInfo.getSerializedSize();
        ++size;
        if (this.columnName != null) {
            size += this.columnName.getBytes(StandardCharsets.UTF_16LE).length;
        }
        return size;
    }

    public void write(RawPacketWriter writer) {
        writer.writeFourByteIntegerLow(this.userType);
        short flags = 0;
        if (this.fNullable) {
            flags = (short)(flags | 1);
        }
        if (this.fCaseSen) {
            flags = (short)(flags | 2);
        }
        flags = (short)(flags | this.usUpdateable << 2);
        if (this.fIdentity) {
            flags = (short)(flags | 0x10);
        }
        if (this.fComputed) {
            flags = (short)(flags | 0x20);
        }
        flags = (short)(flags | this.usReservedODBC << 6);
        if (this.fFixedLenCLRType) {
            flags = (short)(flags | 0x100);
        }
        if (this.fDefault) {
            flags = (short)(flags | 0x200);
        }
        writer.writeTwoByteIntegerLow(flags);
        this.typeInfo.write(writer);
        if (this.columnName != null) {
            byte[] nameBytes = this.columnName.getBytes(StandardCharsets.UTF_16LE);
            writer.writeByte((byte)nameBytes.length);
            writer.writeBytes(nameBytes, 0, nameBytes.length);
        } else {
            writer.writeByte((byte)0);
        }
    }

    public String toString() {
        return "TVP Column Metadata for " + this.columnName + " [" + this.typeInfo.getTypeName() + "]";
    }

    public int getUserType() {
        return this.userType;
    }

    public void setUserType(int userType) {
        this.userType = userType;
    }

    public boolean isfNullable() {
        return this.fNullable;
    }

    public void setfNullable(boolean fNullable) {
        this.fNullable = fNullable;
    }

    public boolean isfCaseSen() {
        return this.fCaseSen;
    }

    public void setfCaseSen(boolean fCaseSen) {
        this.fCaseSen = fCaseSen;
    }

    public byte getUsUpdateable() {
        return this.usUpdateable;
    }

    public void setUsUpdateable(byte usUpdateable) {
        this.usUpdateable = usUpdateable;
    }

    public boolean isfIdentity() {
        return this.fIdentity;
    }

    public void setfIdentity(boolean fIdentity) {
        this.fIdentity = fIdentity;
    }

    public boolean isfComputed() {
        return this.fComputed;
    }

    public void setfComputed(boolean fComputed) {
        this.fComputed = fComputed;
    }

    public byte getUsReservedODBC() {
        return this.usReservedODBC;
    }

    public void setUsReservedODBC(byte usReservedODBC) {
        this.usReservedODBC = usReservedODBC;
    }

    public boolean isfFixedLenCLRType() {
        return this.fFixedLenCLRType;
    }

    public void setfFixedLenCLRType(boolean fFixedLenCLRType) {
        this.fFixedLenCLRType = fFixedLenCLRType;
    }

    public boolean isfDefault() {
        return this.fDefault;
    }

    public void setfDefault(boolean fDefault) {
        this.fDefault = fDefault;
    }

    public byte getUsReserved() {
        return this.usReserved;
    }

    public void setUsReserved(byte usReserved) {
        this.usReserved = usReserved;
    }

    public TypeInfo getTypeInfo() {
        return this.typeInfo;
    }

    public void setTypeInfo(TypeInfo typeInfo) {
        this.typeInfo = typeInfo;
    }

    public String getColumnName() {
        return this.columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }
}
