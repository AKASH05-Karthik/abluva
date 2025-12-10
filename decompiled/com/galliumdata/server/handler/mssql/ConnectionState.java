/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.ServerException;
import com.galliumdata.server.adapters.Variables;
import com.galliumdata.server.handler.mssql.CursorEntry;
import com.galliumdata.server.handler.mssql.MSSQLForwarder;
import com.galliumdata.server.handler.mssql.MSSQLPacket;
import com.galliumdata.server.handler.mssql.SMPPacket;
import com.galliumdata.server.handler.mssql.SMPSession;
import com.galliumdata.server.handler.mssql.tokens.TokenColMetadata;
import com.galliumdata.server.log.Markers;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConnectionState {
    private int tdsMajorVersion = 7;
    private int tdsMinorVersion = 0;
    private int tdsRevision = 0;
    private int packetSize = 8000;
    private String connectionName;
    private String serverName;
    private int serverMajorVersion;
    private boolean columnEncryptionInUse;
    private boolean binaryXml;
    private byte dataClassificationVersion;
    protected Map<Short, SMPSession> smpSessions = new HashMap<Short, SMPSession>();
    private TokenColMetadata lastMetadata;
    private MSSQLPacket lastPacketFromClient;
    protected Variables connectionContext;
    private String lastCursorSql;
    private final Map<Integer, String> openStatements = new HashMap<Integer, String>();
    private static final int MAX_OPEN_PREP_STMTS = 5000;
    private static final Logger protoLog = LogManager.getLogger((String)"galliumdata.dbproto");
    private String lastRPC;
    private int rpcResultIndex;
    private long currentCursorId;
    private TokenColMetadata currentCursorMetadata;
    private final Map<Long, CursorEntry> cursorMetadata = new HashMap<Long, CursorEntry>();
    private final Map<Long, CursorEntry> prepCursorMetadata = new HashMap<Long, CursorEntry>();

    public synchronized int getTdsMajorVersion() {
        return this.tdsMajorVersion;
    }

    public synchronized void setTdsMajorVersion(int tdsMajorVersion) {
        this.tdsMajorVersion = tdsMajorVersion;
    }

    public synchronized int getTdsMinorVersion() {
        return this.tdsMinorVersion;
    }

    public synchronized void setTdsMinorVersion(int tdsMinorVersion) {
        this.tdsMinorVersion = tdsMinorVersion;
    }

    public synchronized int getTdsRevision() {
        return this.tdsRevision;
    }

    public synchronized void setTdsRevision(int rev) {
        this.tdsRevision = rev;
    }

    public synchronized boolean tdsVersion71andHigher() {
        if (this.tdsMajorVersion < 7) {
            return false;
        }
        if (this.tdsMajorVersion > 7) {
            return true;
        }
        return this.tdsMinorVersion >= 1;
    }

    public synchronized boolean tdsVersion71Revision1andHigher() {
        if (this.tdsMajorVersion < 7) {
            return false;
        }
        if (this.tdsMajorVersion > 7) {
            return true;
        }
        if (this.tdsMinorVersion < 1) {
            return false;
        }
        if (this.tdsMinorVersion >= 2) {
            return true;
        }
        return this.tdsRevision >= 1;
    }

    public synchronized boolean tdsVersion71AndLower() {
        if (this.tdsMajorVersion > 7) {
            return false;
        }
        if (this.tdsMajorVersion < 7) {
            return true;
        }
        return this.tdsMinorVersion <= 1;
    }

    public synchronized boolean tdsVersion72andHigher() {
        if (this.tdsMajorVersion < 7) {
            return false;
        }
        if (this.tdsMajorVersion > 7) {
            return true;
        }
        return this.tdsMinorVersion >= 2;
    }

    public synchronized boolean tdsVersion73andHigher() {
        if (this.tdsMajorVersion < 7) {
            return false;
        }
        if (this.tdsMajorVersion > 7) {
            return true;
        }
        return this.tdsMinorVersion >= 3;
    }

    public synchronized boolean tdsVersion73andLower() {
        if (this.tdsMajorVersion > 7) {
            return false;
        }
        if (this.tdsMajorVersion < 7) {
            return true;
        }
        return this.tdsMinorVersion <= 3;
    }

    public synchronized boolean tdsVersion74andHigher() {
        if (this.tdsMajorVersion < 7) {
            return false;
        }
        if (this.tdsMajorVersion > 7) {
            return true;
        }
        return this.tdsMinorVersion >= 4;
    }

    public int getPacketSize() {
        return this.packetSize;
    }

    public void setPacketSize(int packetSize) {
        this.packetSize = packetSize;
    }

    public String getConnectionName() {
        return this.connectionName;
    }

    public void setConnectionName(String connectionName) {
        this.connectionName = connectionName;
    }

    public void setServerName(String name) {
        this.serverName = name;
    }

    public String getServerName() {
        return this.serverName;
    }

    public int getServerMajorVersion() {
        return this.serverMajorVersion;
    }

    public void setServerMajorVersion(int serverMajorVersion) {
        this.serverMajorVersion = serverMajorVersion;
    }

    public boolean isColumnEncryptionInUse() {
        return this.columnEncryptionInUse;
    }

    public void setColumnEncryptionInUse(boolean columnEncryptionInUse) {
        this.columnEncryptionInUse = columnEncryptionInUse;
    }

    public boolean isBinaryXml() {
        return this.binaryXml;
    }

    public void setBinaryXml(boolean binaryXml) {
        this.binaryXml = binaryXml;
    }

    public byte getDataClassificationVersion() {
        return this.dataClassificationVersion;
    }

    public void setDataClassificationVersion(byte dataClassificationVersion) {
        this.dataClassificationVersion = dataClassificationVersion;
    }

    public TokenColMetadata getLastMetadata() {
        return this.lastMetadata;
    }

    public void setLastMetadata(TokenColMetadata lastMetadata) {
        if ("sp_cursoropen".equalsIgnoreCase(this.lastRPC) || "sp_cursorprepexec".equalsIgnoreCase(this.lastRPC)) {
            if (this.currentCursorId == 0L) {
                this.currentCursorMetadata = lastMetadata;
            } else {
                CursorEntry entry = new CursorEntry();
                entry.id = this.currentCursorId;
                entry.sql = this.lastCursorSql;
                entry.metadata = lastMetadata;
                this.cursorMetadata.put(this.currentCursorId, entry);
                this.currentCursorId = 0L;
            }
        } else if ("sp_cursorprepare".equalsIgnoreCase(this.lastRPC) || "sp_cursorexecute".equalsIgnoreCase(this.lastRPC)) {
            if (this.currentCursorId == 0L) {
                this.currentCursorMetadata = lastMetadata;
            } else {
                CursorEntry entry = new CursorEntry();
                entry.id = this.currentCursorId;
                entry.sql = this.lastCursorSql;
                entry.metadata = lastMetadata;
                this.cursorMetadata.put(this.currentCursorId, entry);
                this.currentCursorId = 0L;
            }
        } else {
            this.lastMetadata = lastMetadata;
        }
    }

    public MSSQLPacket getLastPacketFromClient() {
        return this.lastPacketFromClient;
    }

    public void setLastPacketFromClient(MSSQLPacket lastPacketFromClient) {
        this.lastPacketFromClient = lastPacketFromClient;
    }

    public void setConnectionContext(Variables vars) {
        this.connectionContext = vars;
    }

    public void setConnectionContextValue(String name, Object value) {
        this.connectionContext.put(name, value);
    }

    public void startSMPSession(SMPPacket pkt, MSSQLForwarder forwarder) {
        SMPSession session = new SMPSession(pkt, forwarder.connectionContext);
        this.smpSessions.put(session.getSid(), session);
        if (protoLog.isTraceEnabled()) {
            protoLog.trace(Markers.MSSQL, "Starting SMP session " + session.getSid());
        }
    }

    public void closeSMPSession(SMPPacket pkt) {
        SMPSession session = this.smpSessions.get((short)pkt.getSMPSessionId());
        if (session == null) {
            protoLog.trace(Markers.MSSQL, "SMP session close received for unknown session");
            return;
        }
        this.smpSessions.remove((short)pkt.getSMPSessionId());
        if (protoLog.isTraceEnabled()) {
            protoLog.trace(Markers.MSSQL, "Closing SMP session " + pkt.getSMPSessionId());
        }
    }

    public SMPSession getSMPSession(short sid) {
        return this.smpSessions.get(sid);
    }

    public void openPreparedStatement(String sql, int id) {
        this.openStatements.put(id, sql);
        if (this.openStatements.size() > 5000) {
            throw new ServerException("db.mssql.server.TooManyOpenStatements", 5000);
        }
    }

    public void closePreparedStatement(int id) {
        if (!this.openStatements.containsKey(id)) {
            protoLog.debug(Markers.MSSQL, "Closing unknown prepared statement: {}", (Object)id);
        }
        this.openStatements.remove(id);
        protoLog.debug(Markers.MSSQL, "Closed prepared statement {}, still open: {}", (Object)id, (Object)this.openStatements.size());
    }

    public String getPreparedStatement(int id) {
        return this.openStatements.get(id);
    }

    public void setLastRPC(String s) {
        this.lastRPC = s;
        this.rpcResultIndex = 0;
        this.currentCursorId = 0L;
        protoLog.trace(Markers.MSSQL, "Setting last RPC in ConnectionState to: {} for connection: {}", (Object)s, (Object)this.connectionName);
    }

    public String getLastRPC() {
        return this.lastRPC;
    }

    public void clearLastRPC() {
        this.lastRPC = null;
        this.rpcResultIndex = 0;
        this.currentCursorId = 0L;
        this.currentCursorMetadata = null;
    }

    public int getRPCResultIndex() {
        return this.rpcResultIndex;
    }

    public void incrementRPCResultIndex() {
        ++this.rpcResultIndex;
    }

    public void setCurrentCursorID(long id) {
        this.currentCursorId = id;
        if (this.currentCursorMetadata != null) {
            CursorEntry entry = new CursorEntry();
            entry.id = id;
            entry.sql = this.lastCursorSql;
            entry.metadata = this.currentCursorMetadata;
            this.cursorMetadata.put(id, entry);
            protoLog.trace(Markers.MSSQL, "Adding metadata for cursor: {} on connection: {}, SQL: {}", (Object)id, (Object)this.connectionName, (Object)entry.sql);
            this.currentCursorId = 0L;
            this.currentCursorMetadata = null;
            this.lastCursorSql = null;
        } else {
            protoLog.trace(Markers.MSSQL, "Current cursor ID is now: {} for connection: {}, no metadata yet, SQL: {}", (Object)id, (Object)this.connectionName, (Object)this.lastCursorSql);
        }
    }

    public void setCurrentPrepCursorHandle(long id) {
        if (this.currentCursorMetadata != null) {
            CursorEntry entry = new CursorEntry();
            entry.id = id;
            entry.sql = this.lastCursorSql;
            entry.metadata = this.currentCursorMetadata;
            this.prepCursorMetadata.put(id, entry);
            protoLog.trace(Markers.MSSQL, "Adding metadata for prep cursor: {} on connection: {}, SQL: {}", (Object)id, (Object)this.connectionName, (Object)entry.sql);
        } else {
            protoLog.trace(Markers.MSSQL, "Current prep cursor ID is now: {} for connection: {}, no metadata yet, SQL: {}", (Object)id, (Object)this.connectionName, (Object)this.lastCursorSql);
        }
    }

    public String executePrepCursor(long id) {
        if (!this.prepCursorMetadata.containsKey(id)) {
            protoLog.debug(Markers.MSSQL, "sp_cursorexecute called for unknown prepared cursor statement: {}", (Object)id);
            return null;
        }
        CursorEntry entry = this.prepCursorMetadata.get(id);
        this.lastCursorSql = entry.sql;
        this.currentCursorMetadata = entry.metadata;
        return entry.sql;
    }

    public void setLastCursorSql(String s) {
        this.lastCursorSql = s;
    }

    public String useCursor(long id) {
        this.rpcResultIndex = 0;
        if (!this.cursorMetadata.containsKey(id)) {
            protoLog.debug(Markers.MSSQL, "Current cursor ID is unknown: {} for connection: {}", (Object)id, (Object)this.connectionName);
            return null;
        }
        CursorEntry entry = this.cursorMetadata.get(id);
        this.lastMetadata = entry.metadata;
        return entry.sql;
    }

    public void closeCursor(long id) {
        if (protoLog.isDebugEnabled() && !this.cursorMetadata.containsKey(id)) {
            protoLog.debug(Markers.MSSQL, "Unable to get metadata for unknown cursor: {} on connection: {}", (Object)id, (Object)this.connectionName);
        }
        if (!this.cursorMetadata.containsKey(id)) {
            protoLog.debug(Markers.MSSQL, "Unable to close unknown cursor: {} on connection: {}", (Object)id, (Object)this.connectionName);
        }
        this.cursorMetadata.remove(id);
        this.currentCursorId = 0L;
        this.rpcResultIndex = 0;
        if (protoLog.isTraceEnabled()) {
            protoLog.trace(Markers.MSSQL, "Closing cursor: {} for connection: {}", (Object)id, (Object)this.connectionName);
            if (this.cursorMetadata.isEmpty()) {
                protoLog.trace(Markers.MSSQL, "All cursors are closed for connection: {}", (Object)this.connectionName);
            }
        }
    }

    public void closePrepCursor(long id) {
        if (protoLog.isDebugEnabled() && !this.prepCursorMetadata.containsKey(id)) {
            protoLog.debug(Markers.MSSQL, "Unable to get metadata for unknown prep cursor: {} on connection: {}", (Object)id, (Object)this.connectionName);
        }
        if (!this.prepCursorMetadata.containsKey(id)) {
            protoLog.debug(Markers.MSSQL, "Unable to close unknown prep cursor: {} on connection: {}", (Object)id, (Object)this.connectionName);
        }
        this.prepCursorMetadata.remove(id);
        this.rpcResultIndex = 0;
        if (protoLog.isTraceEnabled()) {
            protoLog.trace(Markers.MSSQL, "Closing prep cursor: {} for connection: {}", (Object)id, (Object)this.connectionName);
            if (this.prepCursorMetadata.isEmpty()) {
                protoLog.trace(Markers.MSSQL, "All prep cursors are closed for connection: {}", (Object)this.connectionName);
            }
        }
    }
}
