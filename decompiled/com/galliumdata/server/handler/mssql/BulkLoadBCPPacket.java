/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.handler.mssql.ConnectionState;
import com.galliumdata.server.handler.mssql.MSSQLPacket;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.tokens.ColumnMetadata;
import com.galliumdata.server.handler.mssql.tokens.MessageToken;
import com.galliumdata.server.handler.mssql.tokens.TokenColMetadata;
import com.galliumdata.server.handler.mssql.tokens.TokenDone;
import com.galliumdata.server.handler.mssql.tokens.TokenRow;
import java.util.List;

public class BulkLoadBCPPacket
extends MSSQLPacket {
    private TokenColMetadata currentMetadata;

    public BulkLoadBCPPacket(ConnectionState connectionState) {
        super(connectionState);
        this.typeCode = (byte)7;
    }

    @Override
    public int readFromBytes(byte[] bytes, int offset, int numBytes) {
        int idx = offset;
        return idx - offset;
    }

    @Override
    public void read(RawPacketReader reader) {
        super.read(reader);
    }

    public MessageToken readNextToken(RawPacketReader reader) {
        if (reader.isDone()) {
            return null;
        }
        byte tokenType = reader.readByte();
        MessageToken token = MessageToken.createToken(tokenType, this.connectionState);
        if (token.getTokenType() == -47 || token.getTokenType() == -46) {
            ((TokenRow)token).setColumnMetadata(this.currentMetadata.getColumns());
        }
        if (!(token.getTokenType() != -3 || this.connectionState.tdsVersion72andHigher() && reader.getNumUnreadBytes() != 8)) {
            ((TokenDone)token).setUseShortRowCount(true);
        }
        token.read(reader);
        if (token.getTokenType() == -127) {
            this.currentMetadata = (TokenColMetadata)token;
        }
        return token;
    }

    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        return size;
    }

    @Override
    public void write(RawPacketWriter writer) {
        super.write(writer);
    }

    @Override
    public String getPacketType() {
        return "BulkLoadBCP";
    }

    @Override
    public String toString() {
        List<ColumnMetadata> colMetas;
        if (this.currentMetadata != null && (colMetas = this.currentMetadata.getColumns()) != null && !colMetas.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ColumnMetadata colMeta : colMetas) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(colMeta.getColumnName());
                sb.append(" (");
                sb.append(colMeta.getTypeInfo().getTypeName());
                sb.append(")");
                if (sb.length() <= 100) continue;
                break;
            }
            return "BulkLoadBCP: " + sb.toString();
        }
        return "BulkLoadBCP";
    }
}
