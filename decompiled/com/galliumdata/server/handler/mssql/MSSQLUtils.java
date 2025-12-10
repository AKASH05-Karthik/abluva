/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.handler.mssql.ClientForwarder;
import com.galliumdata.server.handler.mssql.ConnectionState;
import com.galliumdata.server.handler.mssql.MSSQLPacket;
import com.galliumdata.server.handler.mssql.MSSQLResultSet;

public class MSSQLUtils {
    private final ClientForwarder clientForwarder;

    public MSSQLUtils(ClientForwarder clientForwarder) {
        this.clientForwarder = clientForwarder;
    }

    public MSSQLResultSet executeQuery(String sql) {
        ConnectionState connState = this.clientForwarder.getConnectionState();
        MSSQLPacket lastPacket = connState.getLastPacketFromClient();
        MSSQLResultSet rs = this.clientForwarder.queryForRows(lastPacket, sql);
        return rs;
    }
}
