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
import com.galliumdata.server.handler.mssql.MSSQLPacket;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.StreamHeader;
import com.galliumdata.server.handler.mssql.StreamHeaderTxDescriptor;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

public class SQLBatchPacket
extends MSSQLPacket
implements ProxyObject {
    private List<StreamHeader> headers = new ArrayList<StreamHeader>();
    private String sql;

    public SQLBatchPacket(ConnectionState connectionState) {
        super(connectionState);
        this.typeCode = 1;
    }

    @Override
    public int readFromBytes(byte[] bytes, int offset, int numBytes) {
        int idx = offset;
        idx += super.readFromBytes(bytes, offset, numBytes);
        this.connectionState.clearLastRPC();
        if (this.connectionState.tdsVersion72andHigher() && this.getPacketId() == 1) {
            int totalHeaders = DataTypeReader.readFourByteIntegerLow(bytes, idx);
            idx += 4;
            while (idx - offset - 8 < totalHeaders) {
                byte type = bytes[idx + 4];
                StreamHeader header = StreamHeader.createStreamHeader(type);
                idx += header.readFromBytes(bytes, idx, totalHeaders - (idx - offset - 4));
                this.headers.add(header);
            }
        }
        int sqlLen = this.length - (idx - offset);
        this.sql = new String(bytes, idx, sqlLen, StandardCharsets.UTF_16LE);
        return (idx += sqlLen) - offset;
    }

    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        if (this.connectionState.tdsVersion72andHigher()) {
            size += 4;
            for (StreamHeader header : this.headers) {
                size += header.getSerializedSize();
            }
        }
        return size += this.sql.length() * 2;
    }

    @Override
    public void write(RawPacketWriter writer) {
        if (!this.headers.isEmpty()) {
            int headersSize = 4;
            for (StreamHeader header : this.headers) {
                headersSize += header.getSerializedSize();
            }
            writer.writeFourByteIntegerLow(headersSize);
            for (StreamHeader header : this.headers) {
                header.write(writer);
            }
        }
        byte[] sqlBytes = this.sql.getBytes(StandardCharsets.UTF_16LE);
        writer.writeBytes(sqlBytes, 0, sqlBytes.length);
    }

    @Override
    public String getPacketType() {
        return "SQLBatch";
    }

    @Override
    public String toString() {
        Object s = this.sql;
        s = ((String)s).replaceAll("\\v", " ");
        if (((String)(s = ((String)s).replaceAll("  +", " "))).length() > 70) {
            s = ((String)s).substring(0, 70) + "... [" + (this.sql.length() - 70) + " more]";
        }
        return "SQL batch: " + (String)s;
    }

    @Override
    public String toLongString() {
        return "SQL batch: " + this.sql;
    }

    public void copyStreamHeadersFrom(SQLBatchPacket otherPacket) {
        this.headers = otherPacket.headers;
    }

    public void addStreamHeaders() {
        StreamHeaderTxDescriptor header = new StreamHeaderTxDescriptor();
        header.setOutstandingRequestCount(1);
        this.headers.add(header);
    }

    public String getSql() {
        return this.sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    @Override
    public Object getMember(String key) {
        switch (key) {
            case "sql": {
                return this.sql;
            }
            case "packetType": {
                return this.getPacketType();
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

    @Override
    public Object getMemberKeys() {
        return new String[]{"sql", "packetType", "remove", "toString"};
    }

    @Override
    public boolean hasMember(String key) {
        switch (key) {
            case "sql": 
            case "packetType": 
            case "remove": 
            case "toString": {
                return true;
            }
        }
        return false;
    }

    @Override
    public void putMember(String key, Value value) {
        switch (key) {
            case "sql": {
                this.sql = value.asString();
                break;
            }
            default: {
                throw new ServerException("db.mssql.logic.NoSuchMember", key);
            }
        }
    }

    @Override
    public boolean removeMember(String key) {
        throw new ServerException("db.mssql.logic.CannotRemoveMember", key, "SQLBatch packet");
    }
}
