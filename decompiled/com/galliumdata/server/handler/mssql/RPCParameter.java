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
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.TVPTypeInfo;
import com.galliumdata.server.handler.mssql.TypeInfo;
import com.galliumdata.server.handler.mssql.datatypes.MSSQLDataType;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;
import java.nio.charset.StandardCharsets;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

public class RPCParameter
implements ProxyObject {
    private ConnectionState connectionState;
    private final ColumnMetadata columnMetaData;
    private String paramName;
    private boolean fByRefValue;
    private boolean fDefaultValue;
    private boolean fEncrypted;
    private final TypeInfo typeInfo;
    private TVPTypeInfo tvpTypeInfo;
    private MSSQLDataType value;

    public RPCParameter(ConnectionState connectionState) {
        this.columnMetaData = new ColumnMetadata(this.connectionState);
        this.typeInfo = new TypeInfo();
        this.connectionState = connectionState;
    }

    public int readFromBytes(byte[] bytes, int offset) {
        byte paramType;
        int idx = offset;
        byte nameLen = bytes[idx];
        ++idx;
        if (nameLen > 0) {
            this.paramName = new String(bytes, idx, nameLen * 2, StandardCharsets.UTF_16LE);
            idx += nameLen * 2;
        }
        byte statusFlags = bytes[idx];
        this.fByRefValue = (statusFlags & 1) > 0;
        this.fDefaultValue = (statusFlags & 2) > 0;
        this.fEncrypted = (statusFlags & 8) > 0;
        if ((paramType = bytes[++idx]) == -13) {
            this.tvpTypeInfo = new TVPTypeInfo();
            idx += this.tvpTypeInfo.readFromBytes(bytes, idx);
        } else {
            idx += this.typeInfo.readFromBytes(bytes, idx);
            this.columnMetaData.setTypeInfo(this.typeInfo);
            this.value = MSSQLDataType.createDataType(this.columnMetaData);
            this.value.setResizable(true);
            idx += this.value.readFromBytes(bytes, idx);
            this.columnMetaData.setColumnName(this.paramName);
            this.columnMetaData.setTypeInfo(this.typeInfo);
            this.columnMetaData.setConnectionState(this.connectionState);
        }
        return idx - offset;
    }

    public int getSerializedSize() {
        int size = 1;
        if (this.paramName != null) {
            size += this.paramName.length() * 2;
        }
        ++size;
        size += this.typeInfo.getSerializedSize();
        return size += this.value.getSerializedSize();
    }

    public void write(RawPacketWriter writer) {
        if (this.paramName == null) {
            writer.writeByte((byte)0);
        } else {
            writer.writeByte((byte)this.paramName.length());
            byte[] strBytes = this.paramName.getBytes(StandardCharsets.UTF_16LE);
            writer.writeBytes(strBytes, 0, strBytes.length);
        }
        byte statusFlags = 0;
        if (this.fByRefValue) {
            statusFlags = (byte)(statusFlags | 1);
        }
        if (this.fDefaultValue) {
            statusFlags = (byte)(statusFlags | 2);
        }
        if (this.fEncrypted) {
            statusFlags = (byte)(statusFlags | 8);
        }
        writer.writeByte(statusFlags);
        if (this.tvpTypeInfo != null) {
            this.tvpTypeInfo.write(writer);
        } else {
            this.typeInfo.write(writer);
            this.value.write(writer);
        }
    }

    public String toString() {
        Object s;
        Object object = s = this.paramName == null ? "<no name>" : this.paramName;
        if (this.tvpTypeInfo != null) {
            s = (String)s + ": TVP - " + this.tvpTypeInfo.getTvpTypeName() + " - " + this.tvpTypeInfo.getColumnMetas().size() + " columns, " + this.tvpTypeInfo.getTvpRows().size() + " rows";
            return s;
        }
        return (String)s + "=" + String.valueOf(this.getValue());
    }

    public ColumnMetadata getColumnMetaData() {
        return this.columnMetaData;
    }

    public String getParamName() {
        return this.paramName;
    }

    public void setParamName(String paramName) {
        this.paramName = paramName;
    }

    public boolean isfByRefValue() {
        return this.fByRefValue;
    }

    public void setfByRefValue(boolean fByRefValue) {
        this.fByRefValue = fByRefValue;
    }

    public boolean isfDefaultValue() {
        return this.fDefaultValue;
    }

    public void setfDefaultValue(boolean fDefaultValue) {
        this.fDefaultValue = fDefaultValue;
    }

    public boolean isfEncrypted() {
        return this.fEncrypted;
    }

    public void setfEncrypted(boolean fEncrypted) {
        this.fEncrypted = fEncrypted;
    }

    public TypeInfo getTypeInfo() {
        return this.typeInfo;
    }

    public MSSQLDataType getValue() {
        return this.value;
    }

    public void setValue(MSSQLDataType value) {
        this.value = value;
    }

    public Object getMember(String key) {
        switch (key) {
            case "columnMetaData": {
                return this.columnMetaData;
            }
            case "paramName": {
                return this.paramName;
            }
            case "fByRefValue": {
                return this.fByRefValue;
            }
            case "fDefaultValue": {
                return this.fDefaultValue;
            }
            case "fEncrypted": {
                return this.fEncrypted;
            }
            case "typeInfo": {
                return this.typeInfo;
            }
            case "value": {
                return this.value.getValue();
            }
            case "toString": {
                return arguments -> this.toString();
            }
        }
        throw new ServerException("db.mssql.logic.NoSuchMember", key);
    }

    public Object getMemberKeys() {
        return new String[]{"columnMetaData", "paramName", "fByRefValue", "fDefaultValue", "fEncrypted", "typeInfo", "value", "toString"};
    }

    public boolean hasMember(String key) {
        switch (key) {
            case "columnMetaData": 
            case "paramName": 
            case "fByRefValue": 
            case "fDefaultValue": 
            case "fEncrypted": 
            case "typeInfo": 
            case "value": 
            case "toString": {
                return true;
            }
        }
        return false;
    }

    public void putMember(String key, Value val) {
        switch (key) {
            case "paramName": {
                this.paramName = val.asString();
                break;
            }
            case "fByRefValue": {
                this.fByRefValue = val.asBoolean();
                break;
            }
            case "fDefaultValue": {
                this.fDefaultValue = val.asBoolean();
                break;
            }
            case "fEncrypted": {
                this.fEncrypted = val.asBoolean();
                break;
            }
            case "value": {
                this.value.setValueFromJS(val);
                break;
            }
            default: {
                throw new ServerException("db.mssql.logic.NoSuchMember", key);
            }
        }
    }

    public boolean removeMember(String key) {
        throw new ServerException("db.mssql.logic.CannotRemoveMember", key, "RPC packet");
    }
}
