/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.TypeInfo;
import com.galliumdata.server.handler.mssql.datatypes.MSSQLDataType;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;

public class TVPRowValue {
    private final TypeInfo typeInfo;
    protected int len;
    protected MSSQLDataType value;
    private byte extraVariantByte;

    public TVPRowValue(TypeInfo typeInfo) {
        this.typeInfo = typeInfo;
    }

    public int readFromBytes(byte[] bytes, int offset) {
        int idx = offset;
        ColumnMetadata colMeta = new ColumnMetadata(null);
        colMeta.setTypeInfo(this.typeInfo);
        this.value = MSSQLDataType.createDataType(colMeta);
        this.value.setResizable(true);
        if (this.typeInfo.getType() == 106 || this.typeInfo.getType() == 108) {
            this.typeInfo.setVariantScale(6);
        }
        idx += this.value.readFromBytes(bytes, idx);
        return idx - offset;
    }

    public void write(RawPacketWriter writer) {
        this.value.write(writer);
    }
}
