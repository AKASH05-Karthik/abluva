/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.graalvm.polyglot.Value
 *  org.graalvm.polyglot.proxy.ProxyObject
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.ConnectionState;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.DataTypeWriter;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import java.nio.charset.StandardCharsets;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

public class TypeInfo
implements ProxyObject {
    protected ConnectionState connState;
    protected byte type;
    private int varLen;
    private int collationLCID;
    private boolean collationIgnoreCase;
    private boolean collationIgnoreAccent;
    private boolean collationIgnoreWidth;
    private boolean collationIgnoreKana;
    private boolean collationBinary;
    private boolean collationBinary2;
    private boolean collationUTF8;
    private byte collationVersion;
    private byte collationSortId;
    private int precision;
    private int scale;
    private int variantScale;
    private String udtDbName;
    private String udtSchemaName;
    private String udtTypeName;
    private int udtMaxByteSize;
    private String udtAssemblyQualifiedName;
    private boolean xmlSchemaPresent;
    private String xmlDbName;
    private String xmlOwningSchema;
    private String xmlSchemaCollection;

    public TypeInfo() {
    }

    public TypeInfo(ConnectionState connState) {
        this.connState = connState;
    }

    public int readFromBytes(byte[] bytes, int offset) {
        int idx = offset;
        this.type = bytes[idx];
        ++idx;
        idx += this.readDataTypeLength(this.type, bytes, idx);
        switch (this.type) {
            case -89: 
            case -81: 
            case -25: 
            case -17: 
            case 35: 
            case 99: {
                idx += this.readCollation(bytes, idx);
            }
        }
        switch (this.type) {
            case 55: 
            case 63: 
            case 106: 
            case 108: {
                this.precision = bytes[idx];
                this.scale = bytes[++idx];
                ++idx;
            }
        }
        switch (this.type) {
            case 41: 
            case 42: 
            case 43: {
                this.scale = bytes[idx];
                ++idx;
            }
        }
        if (this.type == -16) {
            this.udtMaxByteSize = DataTypeReader.readTwoByteIntegerLow(bytes, idx) & 0xFFFF;
            int strLen = bytes[idx += 2] * 2;
            this.udtDbName = new String(bytes, ++idx, strLen, StandardCharsets.UTF_16LE);
            idx += strLen;
            strLen = bytes[idx] * 2;
            this.udtSchemaName = new String(bytes, ++idx, strLen, StandardCharsets.UTF_16LE);
            idx += strLen;
            strLen = bytes[idx] * 2;
            this.udtTypeName = new String(bytes, ++idx, strLen, StandardCharsets.UTF_16LE);
            idx += strLen;
            strLen = DataTypeReader.readTwoByteIntegerLow(bytes, idx) * 2;
            this.udtAssemblyQualifiedName = new String(bytes, idx += 2, strLen, StandardCharsets.UTF_16LE);
            idx += strLen;
        }
        return idx - offset;
    }

    public void read(RawPacketReader reader) {
        this.type = reader.readByte();
        this.readDataTypeLength(reader, this.type);
        switch (this.type) {
            case -89: 
            case -81: 
            case -25: 
            case -17: 
            case 35: 
            case 99: {
                if (this.connState != null && !this.connState.tdsVersion71andHigher()) break;
                this.readCollation(reader);
            }
        }
        switch (this.type) {
            case 55: 
            case 63: 
            case 106: 
            case 108: {
                this.precision = reader.readByte();
                this.scale = reader.readByte();
            }
        }
        switch (this.type) {
            case 41: 
            case 42: 
            case 43: {
                this.scale = reader.readByte();
            }
        }
        if (this.type == -16) {
            this.udtMaxByteSize = reader.readTwoByteIntLow();
            short strLen = reader.readByte();
            this.udtDbName = reader.readString(strLen * 2);
            strLen = reader.readByte();
            this.udtSchemaName = reader.readString(strLen * 2);
            strLen = reader.readByte();
            this.udtTypeName = reader.readString(strLen * 2);
            strLen = reader.readTwoByteIntLow();
            this.udtAssemblyQualifiedName = reader.readString(strLen * 2);
        }
    }

    public int getSerializedSize() {
        int size = 1;
        switch (this.type) {
            case 31: 
            case 40: 
            case 48: 
            case 50: 
            case 52: 
            case 55: 
            case 56: 
            case 58: 
            case 59: 
            case 60: 
            case 61: 
            case 62: 
            case 63: 
            case 122: 
            case 127: {
                break;
            }
            case 36: 
            case 37: 
            case 38: 
            case 39: 
            case 41: 
            case 42: 
            case 43: 
            case 45: 
            case 47: 
            case 104: 
            case 106: 
            case 108: 
            case 109: 
            case 110: 
            case 111: {
                ++size;
                break;
            }
            case -91: 
            case -89: 
            case -83: 
            case -81: 
            case -25: 
            case -17: {
                size += 2;
                break;
            }
            case -15: {
                ++size;
                if (!this.xmlSchemaPresent) break;
                ++size;
                size += this.xmlDbName.length() * 2;
                ++size;
                size += this.xmlOwningSchema.length() * 2;
                size += 2;
                size += this.xmlSchemaCollection.length() * 2;
                break;
            }
            case 34: 
            case 35: 
            case 98: 
            case 99: {
                size += 4;
                break;
            }
            case -16: {
                break;
            }
            default: {
                throw new ServerException("db.mssql.protocol.UnknownDatatype", this.type);
            }
        }
        if (this.connState == null || this.connState.tdsVersion71andHigher()) {
            switch (this.type) {
                case -89: 
                case -81: 
                case -25: 
                case -17: 
                case 35: 
                case 99: {
                    size += 5;
                }
            }
        }
        switch (this.type) {
            case 55: 
            case 63: 
            case 106: 
            case 108: {
                ++size;
                ++size;
            }
        }
        if (this.type == -16) {
            size += 2;
            ++size;
            size += this.udtDbName.length() * 2;
            ++size;
            size += this.udtSchemaName.length() * 2;
            ++size;
            size += this.udtTypeName.length() * 2;
            size += 2;
            size += this.udtAssemblyQualifiedName.length() * 2;
        }
        return size;
    }

    public void write(RawPacketWriter writer) {
        writer.writeByte(this.type);
        this.writeDataTypeLength(writer);
        if (this.connState == null || this.connState.tdsVersion71andHigher()) {
            switch (this.type) {
                case -89: 
                case -81: 
                case -25: 
                case -17: 
                case 35: 
                case 99: {
                    this.writeCollation(writer);
                }
            }
        }
        switch (this.type) {
            case 55: 
            case 63: 
            case 106: 
            case 108: {
                writer.writeByte((byte)this.precision);
                writer.writeByte((byte)this.scale);
            }
        }
        switch (this.type) {
            case 41: 
            case 42: 
            case 43: {
                writer.writeByte((byte)this.scale);
            }
        }
        if (this.type == -16) {
            writer.writeTwoByteIntegerLow((short)this.udtMaxByteSize);
            writer.writeByte((byte)this.udtDbName.length());
            byte[] strBytes = this.udtDbName.getBytes(StandardCharsets.UTF_16LE);
            writer.writeBytes(strBytes, 0, strBytes.length);
            writer.writeByte((byte)this.udtSchemaName.length());
            strBytes = this.udtSchemaName.getBytes(StandardCharsets.UTF_16LE);
            writer.writeBytes(strBytes, 0, strBytes.length);
            writer.writeByte((byte)this.udtTypeName.length());
            strBytes = this.udtTypeName.getBytes(StandardCharsets.UTF_16LE);
            writer.writeBytes(strBytes, 0, strBytes.length);
            writer.writeTwoByteIntegerLow(this.udtAssemblyQualifiedName.length());
            strBytes = this.udtAssemblyQualifiedName.getBytes(StandardCharsets.UTF_16LE);
            writer.writeBytes(strBytes, 0, strBytes.length);
        }
    }

    protected int readDataTypeLength(byte type, byte[] bytes, int idx) {
        switch (type) {
            case 31: 
            case 40: 
            case 41: 
            case 42: 
            case 43: 
            case 48: 
            case 50: 
            case 52: 
            case 55: 
            case 56: 
            case 58: 
            case 59: 
            case 60: 
            case 61: 
            case 62: 
            case 63: 
            case 122: 
            case 127: {
                return 0;
            }
            case 36: 
            case 37: 
            case 38: 
            case 39: 
            case 45: 
            case 47: 
            case 104: 
            case 106: 
            case 108: 
            case 109: 
            case 110: 
            case 111: {
                this.varLen = bytes[idx];
                return 1;
            }
            case -91: 
            case -89: 
            case -83: 
            case -81: 
            case -25: 
            case -17: {
                this.varLen = DataTypeReader.readTwoByteIntegerLow(bytes, idx) & 0xFFFF;
                return 2;
            }
            case -15: {
                int initIdx = idx;
                this.xmlSchemaPresent = bytes[idx] != 0;
                ++idx;
                if (this.xmlSchemaPresent) {
                    byte dbNameLen = bytes[idx];
                    this.xmlDbName = new String(bytes, ++idx, dbNameLen * 2, StandardCharsets.UTF_16LE);
                    byte schemaNameLen = bytes[idx += dbNameLen * 2];
                    this.xmlOwningSchema = new String(bytes, ++idx, schemaNameLen * 2, StandardCharsets.UTF_16LE);
                    short collNameLen = DataTypeReader.readTwoByteIntegerLow(bytes, idx += schemaNameLen * 2);
                    this.xmlSchemaCollection = new String(bytes, idx += 2, collNameLen * 2, StandardCharsets.UTF_16LE);
                    idx += collNameLen * 2;
                }
                return idx - initIdx;
            }
            case 34: 
            case 35: 
            case 98: 
            case 99: {
                this.varLen = DataTypeReader.readFourByteIntegerLow(bytes, idx);
                return 4;
            }
            case -16: {
                return 0;
            }
            case -13: {
                return 0;
            }
        }
        throw new ServerException("db.mssql.protocol.UnknownDatatype", type);
    }

    protected void readDataTypeLength(RawPacketReader reader, byte type) {
        switch (type) {
            case 31: 
            case 40: 
            case 41: 
            case 42: 
            case 43: 
            case 48: 
            case 50: 
            case 52: 
            case 55: 
            case 56: 
            case 58: 
            case 59: 
            case 60: 
            case 61: 
            case 62: 
            case 63: 
            case 122: 
            case 127: {
                return;
            }
            case 36: 
            case 37: 
            case 38: 
            case 39: 
            case 45: 
            case 47: 
            case 104: 
            case 106: 
            case 108: 
            case 109: 
            case 110: 
            case 111: {
                this.varLen = reader.readByte();
                return;
            }
            case -91: 
            case -89: 
            case -83: 
            case -81: 
            case -25: 
            case -17: {
                this.varLen = reader.readTwoByteIntLow();
                return;
            }
            case -15: {
                boolean bl = this.xmlSchemaPresent = reader.readByte() != 0;
                if (this.xmlSchemaPresent) {
                    byte dbNameLen = reader.readByte();
                    this.xmlDbName = reader.readString(dbNameLen * 2);
                    byte schemaNameLen = reader.readByte();
                    this.xmlOwningSchema = reader.readString(schemaNameLen * 2);
                    short collNameLen = reader.readTwoByteIntLow();
                    this.xmlSchemaCollection = reader.readString(collNameLen * 2);
                }
                return;
            }
            case 34: 
            case 35: 
            case 98: 
            case 99: {
                this.varLen = reader.readFourByteIntLow();
                return;
            }
            case -16: {
                return;
            }
            case -13: {
                return;
            }
        }
        throw new ServerException("db.mssql.protocol.UnknownDatatype", type);
    }

    public int readCollation(byte[] bytes, int offset) {
        int idx = offset;
        this.collationLCID = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        this.collationLCID &= 0xFFFFF;
        byte collationFlagsLow = bytes[idx += 2];
        byte collationFlagsHigh = bytes[++idx];
        this.collationIgnoreCase = (collationFlagsLow & 0x10) != 0;
        this.collationIgnoreAccent = (collationFlagsLow & 0x20) != 0;
        this.collationIgnoreWidth = (collationFlagsLow & 0x40) != 0;
        this.collationIgnoreKana = (collationFlagsLow & 0x80) != 0;
        this.collationBinary = (collationFlagsHigh & 1) != 0;
        this.collationBinary2 = (collationFlagsHigh & 2) != 0;
        this.collationUTF8 = (collationFlagsHigh & 4) != 0;
        this.collationVersion = (byte)((collationFlagsHigh & 0xF0) >> 4);
        this.collationSortId = bytes[++idx];
        return ++idx - offset;
    }

    public void readCollation(RawPacketReader reader) {
        this.collationLCID = reader.readFourByteIntLow();
        byte collationFlagsLow = (byte)((this.collationLCID & 0xFF0000) >> 16);
        byte collationFlagsHigh = (byte)((this.collationLCID & 0xFF000000) >> 24);
        this.collationLCID &= 0xFFFFF;
        this.collationIgnoreCase = (collationFlagsLow & 0x10) != 0;
        this.collationIgnoreAccent = (collationFlagsLow & 0x20) != 0;
        this.collationIgnoreWidth = (collationFlagsLow & 0x40) != 0;
        this.collationIgnoreKana = (collationFlagsLow & 0x80) != 0;
        this.collationBinary = (collationFlagsHigh & 1) != 0;
        this.collationBinary2 = (collationFlagsHigh & 2) != 0;
        this.collationUTF8 = (collationFlagsHigh & 4) != 0;
        this.collationVersion = (byte)((collationFlagsHigh & 0xF0) >> 4);
        this.collationSortId = reader.readByte();
    }

    private void writeDataTypeLength(RawPacketWriter writer) {
        switch (this.type) {
            case 31: 
            case 40: 
            case 41: 
            case 42: 
            case 43: 
            case 48: 
            case 50: 
            case 52: 
            case 55: 
            case 56: 
            case 58: 
            case 59: 
            case 60: 
            case 61: 
            case 62: 
            case 63: 
            case 122: 
            case 127: {
                break;
            }
            case 36: 
            case 37: 
            case 38: 
            case 39: 
            case 45: 
            case 47: 
            case 104: 
            case 106: 
            case 108: 
            case 109: 
            case 110: 
            case 111: {
                writer.writeByte((byte)this.varLen);
                break;
            }
            case -91: 
            case -89: 
            case -83: 
            case -81: 
            case -25: 
            case -17: {
                writer.writeTwoByteIntegerLow((short)this.varLen);
                break;
            }
            case -15: {
                writer.writeByte(this.xmlSchemaPresent ? (byte)1 : 0);
                if (!this.xmlSchemaPresent) break;
                byte[] strBytes = this.xmlDbName.getBytes(StandardCharsets.UTF_16LE);
                writer.writeByte((byte)(strBytes.length / 2));
                writer.writeBytes(strBytes, 0, strBytes.length);
                strBytes = this.xmlOwningSchema.getBytes(StandardCharsets.UTF_16LE);
                writer.writeByte((byte)(strBytes.length / 2));
                writer.writeBytes(strBytes, 0, strBytes.length);
                strBytes = this.xmlSchemaCollection.getBytes(StandardCharsets.UTF_16LE);
                writer.writeTwoByteIntegerLow(strBytes.length / 2);
                writer.writeBytes(strBytes, 0, strBytes.length);
                break;
            }
            case 34: 
            case 35: 
            case 98: 
            case 99: {
                writer.writeFourByteIntegerLow(this.varLen);
                break;
            }
            case -16: 
            case -13: {
                return;
            }
            default: {
                throw new ServerException("db.mssql.protocol.UnknownDatatype", this.type);
            }
        }
    }

    public void writeCollation(RawPacketWriter writer) {
        byte[] bytes = new byte[5];
        DataTypeWriter.encodeFourByteIntegerLow(bytes, 0, this.collationLCID & 0xFFFFF);
        if (this.collationIgnoreCase) {
            bytes[2] = (byte)(bytes[2] | 0x10);
        }
        if (this.collationIgnoreAccent) {
            bytes[2] = (byte)(bytes[2] | 0x20);
        }
        if (this.collationIgnoreWidth) {
            bytes[2] = (byte)(bytes[2] | 0x40);
        }
        if (this.collationIgnoreKana) {
            bytes[2] = (byte)(bytes[2] | 0x80);
        }
        if (this.collationBinary) {
            bytes[3] = (byte)(bytes[3] | 1);
        }
        if (this.collationBinary2) {
            bytes[3] = (byte)(bytes[3] | 2);
        }
        if (this.collationUTF8) {
            bytes[3] = (byte)(bytes[3] | 4);
        }
        bytes[3] = (byte)(bytes[3] | (byte)(this.collationVersion << 4));
        bytes[4] = this.collationSortId;
        writer.writeBytes(bytes, 0, 5);
    }

    public String toString() {
        return "TypeInfo for " + this.getTypeName();
    }

    public int hashCode() {
        int hash = this.type + this.varLen;
        hash += this.precision * 17;
        hash += this.scale * 31;
        hash += this.collationLCID * 7;
        return hash += this.collationSortId * 257;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof TypeInfo)) {
            return false;
        }
        TypeInfo other = (TypeInfo)obj;
        if (other.type != this.type) {
            return false;
        }
        if (other.varLen != this.varLen) {
            return false;
        }
        if (other.precision != this.precision) {
            return false;
        }
        if (other.scale != this.scale) {
            return false;
        }
        if (other.collationLCID != this.collationLCID) {
            return false;
        }
        return other.collationSortId == this.collationSortId;
    }

    public byte getType() {
        return this.type;
    }

    public String getTypeName() {
        switch (this.type) {
            case 31: {
                return "null";
            }
            case 48: {
                return "tinyint";
            }
            case 50: {
                return "bit";
            }
            case 52: {
                return "smallint";
            }
            case 56: {
                return "int";
            }
            case 58: {
                return "smalldatetime";
            }
            case 59: {
                return "real";
            }
            case 60: {
                return "money";
            }
            case 61: {
                return "datetime";
            }
            case 62: {
                return "float";
            }
            case 122: {
                return "smallmoney";
            }
            case 127: {
                return "bigint";
            }
            case 55: {
                return "decimal (legacy)";
            }
            case 63: {
                return "numeric (legacy)";
            }
            case 36: {
                return "uniqueidentifier";
            }
            case 38: {
                return "intn";
            }
            case 104: {
                return "bitn";
            }
            case 106: {
                return "decimal";
            }
            case 108: {
                return "numeric";
            }
            case 109: {
                return "floatn";
            }
            case 110: {
                return "moneyn";
            }
            case 111: {
                return "datetimen";
            }
            case 40: {
                return "daten";
            }
            case 41: {
                return "timen";
            }
            case 42: {
                return "datetime2n";
            }
            case 43: {
                return "datetimeoffsetn";
            }
            case 47: {
                return "char (legacy)";
            }
            case 39: {
                return "varchar (legacy)";
            }
            case 45: {
                return "binary (legacy)";
            }
            case 37: {
                return "varbinary (legacy)";
            }
            case -91: {
                return "varbinary";
            }
            case -89: {
                return "varchar";
            }
            case -83: {
                return "binary";
            }
            case -81: {
                return "char";
            }
            case -25: {
                return "nvarchar";
            }
            case -17: {
                return "nchar";
            }
            case -15: {
                return "xml";
            }
            case -16: {
                return "udt " + this.udtTypeName;
            }
            case 35: {
                return "text";
            }
            case 34: {
                return "image";
            }
            case 99: {
                return "ntext";
            }
            case 98: {
                return "sql_variant";
            }
        }
        return "unknown: " + this.type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public int getVarLen() {
        return this.varLen;
    }

    public void setVarLen(int varLen) {
        this.varLen = varLen;
    }

    public boolean varLenIsUnlimited() {
        return this.varLen == 65535 || this.varLen == -1 || this.varLen == 0x7FFFFFFE;
    }

    public int getCollationLCID() {
        return this.collationLCID;
    }

    public void setCollationLCID(int collationLCID) {
        this.collationLCID = collationLCID;
    }

    public boolean isCollationIgnoreCase() {
        return this.collationIgnoreCase;
    }

    public void setCollationIgnoreCase(boolean collationIgnoreCase) {
        this.collationIgnoreCase = collationIgnoreCase;
    }

    public boolean isCollationIgnoreAccent() {
        return this.collationIgnoreAccent;
    }

    public void setCollationIgnoreAccent(boolean collationIgnoreAccent) {
        this.collationIgnoreAccent = collationIgnoreAccent;
    }

    public boolean isCollationIgnoreWidth() {
        return this.collationIgnoreWidth;
    }

    public void setCollationIgnoreWidth(boolean collationIgnoreWidth) {
        this.collationIgnoreWidth = collationIgnoreWidth;
    }

    public boolean isCollationIgnoreKana() {
        return this.collationIgnoreKana;
    }

    public void setCollationIgnoreKana(boolean collationIgnoreKana) {
        this.collationIgnoreKana = collationIgnoreKana;
    }

    public boolean isCollationBinary() {
        return this.collationBinary;
    }

    public void setCollationBinary(boolean collationBinary) {
        this.collationBinary = collationBinary;
    }

    public boolean isCollationBinary2() {
        return this.collationBinary2;
    }

    public void setCollationBinary2(boolean collationBinary2) {
        this.collationBinary2 = collationBinary2;
    }

    public boolean isCollationUTF8() {
        return this.collationUTF8;
    }

    public void setCollationUTF8(boolean collationUTF8) {
        this.collationUTF8 = collationUTF8;
    }

    public byte getCollationVersion() {
        return this.collationVersion;
    }

    public void setCollationVersion(byte collationVersion) {
        this.collationVersion = collationVersion;
    }

    public byte getCollationSortId() {
        return this.collationSortId;
    }

    public void setCollationSortId(byte collationSortId) {
        this.collationSortId = collationSortId;
    }

    public int getPrecision() {
        return this.precision;
    }

    public void setPrecision(int precision) {
        this.precision = precision;
    }

    public int getScale() {
        return this.scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public int getVariantScale() {
        return this.variantScale;
    }

    public void setVariantScale(int variantScale) {
        this.variantScale = variantScale;
    }

    public String getUdtDbName() {
        return this.udtDbName;
    }

    public void setUdtDbName(String udtDbName) {
        this.udtDbName = udtDbName;
    }

    public String getUdtSchemaName() {
        return this.udtSchemaName;
    }

    public void setUdtSchemaName(String udtSchemaName) {
        this.udtSchemaName = udtSchemaName;
    }

    public String getUdtTypeName() {
        return this.udtTypeName;
    }

    public void setUdtTypeName(String udtTypeName) {
        this.udtTypeName = udtTypeName;
    }

    public int getUdtMaxByteSize() {
        return this.udtMaxByteSize;
    }

    public void setUdtMaxByteSize(int udtMaxByteSize) {
        this.udtMaxByteSize = udtMaxByteSize;
    }

    public String getUdtAssemblyQualifiedName() {
        return this.udtAssemblyQualifiedName;
    }

    public void setUdtAssemblyQualifiedName(String udtAssemblyQualifiedName) {
        this.udtAssemblyQualifiedName = udtAssemblyQualifiedName;
    }

    public boolean getXmlSchemaPresent() {
        return this.xmlSchemaPresent;
    }

    public void setXmlSchemaPresent(boolean xmlSchemaPresent) {
        this.xmlSchemaPresent = xmlSchemaPresent;
    }

    public String getXmlDbName() {
        return this.xmlDbName;
    }

    public void setXmlDbName(String xmlDbName) {
        this.xmlDbName = xmlDbName;
    }

    public String getXmlOwningSchema() {
        return this.xmlOwningSchema;
    }

    public void setXmlOwningSchema(String xmlOwningSchema) {
        this.xmlOwningSchema = xmlOwningSchema;
    }

    public String getXmlSchemaCollection() {
        return this.xmlSchemaCollection;
    }

    public void setXmlSchemaCollection(String xmlSchemaCollection) {
        this.xmlSchemaCollection = xmlSchemaCollection;
    }

    public Object getMember(String key) {
        switch (key) {
            case "type": {
                return this.type;
            }
            case "typeName": {
                return this.getTypeName();
            }
            case "varLen": {
                return this.varLen;
            }
            case "collationLCID": {
                return this.collationLCID;
            }
            case "collationIgnoreCase": {
                return this.collationIgnoreCase;
            }
            case "collationIgnoreAccent": {
                return this.collationIgnoreAccent;
            }
            case "collationIgnoreWidth": {
                return this.collationIgnoreWidth;
            }
            case "collationIgnoreKana": {
                return this.collationIgnoreKana;
            }
            case "collationBinary": {
                return this.collationBinary;
            }
            case "collationBinary2": {
                return this.collationBinary2;
            }
            case "collationUTF8": {
                return this.collationUTF8;
            }
            case "collationVersion": {
                return this.collationVersion;
            }
            case "collationSortId": {
                return this.collationSortId;
            }
            case "precision": {
                return this.precision;
            }
            case "scale": {
                return this.scale;
            }
            case "udtDbName": {
                return this.udtDbName;
            }
            case "udtSchemaName": {
                return this.udtSchemaName;
            }
            case "udtTypeName": {
                return this.udtTypeName;
            }
            case "udtMaxByteSize": {
                return this.udtMaxByteSize;
            }
            case "udtAssemblyQualifiedName": {
                return this.udtAssemblyQualifiedName;
            }
            case "xmlSchemaPresent": {
                return this.xmlSchemaPresent;
            }
            case "xmlDbName": {
                return this.xmlDbName;
            }
            case "xmlOwningSchema": {
                return this.xmlOwningSchema;
            }
            case "xmlSchemaCollection": {
                return this.xmlSchemaCollection;
            }
            case "toString": {
                return arguments -> this.toString();
            }
        }
        throw new ServerException("db.mssql.logic.NoSuchMember", key);
    }

    public Object getMemberKeys() {
        return new String[]{"type", "typeName", "varLen", "collationLCID", "collationIgnoreCase", "collationIgnoreAccent", "collationIgnoreWidth", "collationIgnoreKana", "collationBinary", "collationBinary2", "collationUTF8", "collationVersion", "collationSortId", "precision", "scale", "udtDbName", "udtSchemaName", "udtTypeName", "udtMaxByteSize", "udtAssemblyQualifiedName", "xmlSchemaPresent", "xmlDbName", "xmlOwningSchema", "xmlSchemaCollection", "toString"};
    }

    public boolean hasMember(String key) {
        switch (key) {
            case "type": 
            case "typeName": 
            case "varLen": 
            case "collationLCID": 
            case "collationIgnoreCase": 
            case "collationIgnoreAccent": 
            case "collationIgnoreWidth": 
            case "collationIgnoreKana": 
            case "collationBinary": 
            case "collationBinary2": 
            case "collationUTF8": 
            case "collationVersion": 
            case "collationSortId": 
            case "precision": 
            case "scale": 
            case "udtDbName": 
            case "udtSchemaName": 
            case "udtTypeName": 
            case "udtMaxByteSize": 
            case "udtAssemblyQualifiedName": 
            case "xmlSchemaPresent": 
            case "xmlDbName": 
            case "xmlOwningSchema": 
            case "xmlSchemaCollection": 
            case "toString": {
                return true;
            }
        }
        return false;
    }

    public void putMember(String key, Value val) {
        switch (key) {
            case "type": {
                this.setType(val.asByte());
                break;
            }
            case "varLen": {
                this.setVarLen(val.asInt());
                break;
            }
            case "collationLCID": {
                this.setCollationLCID(val.asInt());
                break;
            }
            case "collationIgnoreCase": {
                this.setCollationIgnoreCase(val.asBoolean());
                break;
            }
            case "collationIgnoreAccent": {
                this.setCollationIgnoreAccent(val.asBoolean());
                break;
            }
            case "collationIgnoreWidth": {
                this.setCollationIgnoreWidth(val.asBoolean());
                break;
            }
            case "collationIgnoreKana": {
                this.setCollationIgnoreKana(val.asBoolean());
                break;
            }
            case "collationBinary": {
                this.setCollationBinary(val.asBoolean());
                break;
            }
            case "collationBinary2": {
                this.setCollationBinary2(val.asBoolean());
                break;
            }
            case "collationUTF8": {
                this.setCollationUTF8(val.asBoolean());
                break;
            }
            case "collationVersion": {
                this.setCollationVersion(val.asByte());
                break;
            }
            case "collationSortId": {
                this.setCollationSortId(val.asByte());
                break;
            }
            case "precision": {
                this.setPrecision(val.asInt());
                break;
            }
            case "scale": {
                this.setScale(val.asInt());
                break;
            }
            case "udtDbName": {
                this.setUdtDbName(val.asString());
                break;
            }
            case "udtSchemaName": {
                this.setUdtSchemaName(val.asString());
            }
            case "udtTypeName": {
                this.setUdtTypeName(val.asString());
            }
            case "udtMaxByteSize": {
                this.setUdtMaxByteSize(val.asInt());
                break;
            }
            case "udtAssemblyQualifiedName": {
                this.setUdtAssemblyQualifiedName(val.asString());
                break;
            }
            case "xmlSchemaPresent": {
                this.setXmlSchemaPresent(val.asBoolean());
                break;
            }
            case "xmlDbName": {
                this.setXmlDbName(val.asString());
                break;
            }
            case "xmlOwningSchema": {
                this.setXmlOwningSchema(val.asString());
                break;
            }
            case "xmlSchemaCollection": {
                this.setXmlSchemaCollection(val.asString());
            }
            default: {
                throw new ServerException("db.mssql.logic.NoSuchMember", key);
            }
        }
    }

    public boolean removeMember(String key) {
        throw new ServerException("db.mssql.logic.CannotRemoveMember", key, "TypeInfo");
    }
}
