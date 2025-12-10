/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.TVPRowValue;
import com.galliumdata.server.handler.mssql.TVPTypeInfo;
import com.galliumdata.server.handler.mssql.TVPTypeInfoColumnMetadata;
import java.util.ArrayList;
import java.util.List;

public class TVPRow {
    private final TVPTypeInfo typeInfo;
    private final List<TVPRowValue> values = new ArrayList<TVPRowValue>();

    public TVPRow(TVPTypeInfo typeInfo) {
        this.typeInfo = typeInfo;
    }

    public int readFromBytes(byte[] bytes, int offset) {
        int idx = offset;
        List<TVPTypeInfoColumnMetadata> metas = this.typeInfo.getColumnMetas();
        for (TVPTypeInfoColumnMetadata meta : metas) {
            TVPRowValue value = new TVPRowValue(meta.getTypeInfo());
            idx += value.readFromBytes(bytes, idx);
            this.values.add(value);
        }
        return idx - offset;
    }

    public void write(RawPacketWriter writer) {
        writer.writeByte((byte)1);
        for (TVPRowValue value : this.values) {
            value.write(writer);
        }
    }

    public String toString() {
        Object s = "TVPRow: ";
        for (int i = 0; i < this.typeInfo.getColumnMetas().size(); ++i) {
            TVPTypeInfoColumnMetadata meta = this.typeInfo.getColumnMetas().get(i);
            TVPRowValue value = this.values.get(i);
            String name = meta.getColumnName();
            if (name == null) {
                name = meta.getTypeInfo().getTypeName();
            }
            s = (String)s + name + "=" + String.valueOf(value.value) + ", ";
        }
        return s;
    }
}
