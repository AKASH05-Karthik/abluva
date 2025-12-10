/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.TVPColumnOrderingToken;
import com.galliumdata.server.handler.mssql.TVPOrderUniqueToken;
import com.galliumdata.server.handler.mssql.TVPRow;
import com.galliumdata.server.handler.mssql.TVPTypeInfoColumnMetadata;
import com.galliumdata.server.handler.mssql.TypeInfo;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TVPTypeInfo
extends TypeInfo {
    private String tvpDbName;
    private String tvpOwningSchema;
    private String tvpTypeName;
    private List<TVPTypeInfoColumnMetadata> columnMetas = new ArrayList<TVPTypeInfoColumnMetadata>();
    private TVPOrderUniqueToken orderUniqueToken;
    private TVPColumnOrderingToken columnOrderToken;
    private final List<TVPRow> tvpRows = new ArrayList<TVPRow>();

    @Override
    public int readFromBytes(byte[] bytes, int offset) {
        int idx = offset;
        byte len = bytes[idx += super.readFromBytes(bytes, offset)];
        ++idx;
        if (len > 0) {
            this.tvpDbName = new String(bytes, idx, len * 2, StandardCharsets.UTF_16LE);
            idx += len * 2;
        }
        len = bytes[idx];
        ++idx;
        if (len > 0) {
            this.tvpOwningSchema = new String(bytes, idx, len * 2, StandardCharsets.UTF_16LE);
            idx += len * 2;
        }
        len = bytes[idx];
        ++idx;
        if (len > 0) {
            this.tvpTypeName = new String(bytes, idx, len * 2, StandardCharsets.UTF_16LE);
            idx += len * 2;
        }
        int metaCnt = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        idx += 2;
        if (metaCnt == -1) {
            metaCnt = 0;
        }
        for (int i = 0; i < metaCnt; ++i) {
            TVPTypeInfoColumnMetadata meta = new TVPTypeInfoColumnMetadata();
            idx += meta.readFromBytes(bytes, idx, bytes.length);
            this.columnMetas.add(meta);
        }
        byte tokenType = bytes[idx];
        ++idx;
        while (tokenType != 0) {
            if (tokenType == 16) {
                this.orderUniqueToken = new TVPOrderUniqueToken();
                idx += this.orderUniqueToken.readFromBytes(bytes, idx);
            }
            if (tokenType == 17) {
                this.columnOrderToken = new TVPColumnOrderingToken();
                idx += this.columnOrderToken.readFromBytes(bytes, idx);
            }
            tokenType = bytes[idx];
            ++idx;
        }
        tokenType = bytes[idx];
        ++idx;
        while (tokenType != 0) {
            if (tokenType != 1) {
                throw new ServerException("db.mssql.protocol.BadTVPMetadata", "unknown token type (expected 1): " + tokenType);
            }
            TVPRow row = new TVPRow(this);
            idx += row.readFromBytes(bytes, idx);
            this.tvpRows.add(row);
            tokenType = bytes[idx];
            ++idx;
        }
        return idx - offset;
    }

    @Override
    protected int readDataTypeLength(byte type, byte[] bytes, int idx) {
        return super.readDataTypeLength(type, bytes, idx);
    }

    @Override
    public void write(RawPacketWriter writer) {
        byte[] nameBytes;
        super.write(writer);
        if (this.tvpDbName != null) {
            nameBytes = this.tvpDbName.getBytes(StandardCharsets.UTF_16LE);
            writer.writeByte((byte)(nameBytes.length / 2));
            writer.writeBytes(nameBytes, 0, nameBytes.length);
        } else {
            writer.writeByte((byte)0);
        }
        if (this.tvpOwningSchema != null) {
            nameBytes = this.tvpOwningSchema.getBytes(StandardCharsets.UTF_16LE);
            writer.writeByte((byte)(nameBytes.length / 2));
            writer.writeBytes(nameBytes, 0, nameBytes.length);
        } else {
            writer.writeByte((byte)0);
        }
        if (this.tvpTypeName != null) {
            nameBytes = this.tvpTypeName.getBytes(StandardCharsets.UTF_16LE);
            writer.writeByte((byte)(nameBytes.length / 2));
            writer.writeBytes(nameBytes, 0, nameBytes.length);
        } else {
            writer.writeByte((byte)0);
        }
        writer.writeTwoByteIntegerLow(this.columnMetas.size());
        for (TVPTypeInfoColumnMetadata meta : this.columnMetas) {
            meta.write(writer);
        }
        if (this.orderUniqueToken != null) {
            this.orderUniqueToken.write(writer);
        }
        if (this.columnOrderToken != null) {
            this.columnOrderToken.write(writer);
        }
        writer.writeByte((byte)0);
        for (TVPRow row : this.tvpRows) {
            row.write(writer);
        }
        writer.writeByte((byte)0);
    }

    @Override
    public String toString() {
        return "TVPTypeInfo: " + this.tvpTypeName;
    }

    public List<TVPTypeInfoColumnMetadata> getColumnMetas() {
        return this.columnMetas;
    }

    public void setColumnMetas(List<TVPTypeInfoColumnMetadata> columnMetas) {
        this.columnMetas = columnMetas;
    }

    public String getTvpDbName() {
        return this.tvpDbName;
    }

    public void setTvpDbName(String tvpDbName) {
        this.tvpDbName = tvpDbName;
    }

    public String getTvpOwningSchema() {
        return this.tvpOwningSchema;
    }

    public void setTvpOwningSchema(String tvpOwningSchema) {
        this.tvpOwningSchema = tvpOwningSchema;
    }

    public String getTvpTypeName() {
        return this.tvpTypeName;
    }

    public void setTvpTypeName(String tvpTypeName) {
        this.tvpTypeName = tvpTypeName;
    }

    public TVPOrderUniqueToken getOrderUniqueToken() {
        return this.orderUniqueToken;
    }

    public void setOrderUniqueToken(TVPOrderUniqueToken orderUniqueToken) {
        this.orderUniqueToken = orderUniqueToken;
    }

    public TVPColumnOrderingToken getColumnOrderToken() {
        return this.columnOrderToken;
    }

    public void setColumnOrderToken(TVPColumnOrderingToken columnOrderToken) {
        this.columnOrderToken = columnOrderToken;
    }

    public List<TVPRow> getTvpRows() {
        return this.tvpRows;
    }
}
