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
import com.galliumdata.server.handler.mssql.SMPPacket;
import com.galliumdata.server.log.Markers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SMPSession {
    private short sid;
    private int clientSeqNum = 1;
    private int serverSeqNum = 1;
    private int clientWindow;
    private int serverWindow;
    private Variables smpConnectionContext = new Variables();
    private static final Logger log = LogManager.getLogger((String)"galliumdata.dbproto");

    public SMPSession(SMPPacket pkt, Variables connectionContext) {
        if (pkt.getSerializedSize() != 16) {
            throw new ServerException("db.mssql.protocol.BadSMPSyn", "Unexpected size, expected 16, got: " + pkt.getSerializedSize());
        }
        if (pkt.getSMPPacketType() != SMPPacket.SMPPacketType.SYN) {
            throw new ServerException("db.mssql.protocol.BadSMPSyn", "SYN has type " + String.valueOf((Object)pkt.getSMPPacketType()));
        }
        if (pkt.getSeqNum() != 0) {
            throw new ServerException("db.mssql.protocol.BadSMPSyn", "SYN does not have seqNum==0 but " + pkt.getSeqNum());
        }
        this.sid = (short)pkt.getSMPSessionId();
        this.clientWindow = pkt.getSMPWindow();
        for (String key : connectionContext.keySet()) {
            this.smpConnectionContext.put(key, connectionContext.get(key));
        }
    }

    public short getSid() {
        return this.sid;
    }

    public void setSid(short sid) {
        this.sid = sid;
    }

    public int getClientSeqNum() {
        return this.clientSeqNum;
    }

    public int getAndIncrementClientSeqNum() {
        int seq = this.clientSeqNum++;
        return seq;
    }

    public void setClientSeqNum(int clientSeqNum) {
        this.clientSeqNum = clientSeqNum;
    }

    public int getServerSeqNum() {
        return this.serverSeqNum;
    }

    public int getAndIncrementServerSeqNum() {
        int seq = this.serverSeqNum++;
        return seq;
    }

    public void setServerSeqNum(int serverSeqNum) {
        this.serverSeqNum = serverSeqNum;
    }

    public int getClientWindow() {
        return this.clientWindow;
    }

    public void setClientWindow(int clientWindow) {
        if (log.isTraceEnabled()) {
            log.trace(Markers.MSSQL, "SMP Session: setting client window to " + clientWindow);
        }
        this.clientWindow = clientWindow;
    }

    public int getServerWindow() {
        return this.serverWindow;
    }

    public void setServerWindow(int serverWindow) {
        if (log.isTraceEnabled()) {
            log.trace(Markers.MSSQL, "SMP Session: setting server window to " + serverWindow);
        }
        this.serverWindow = serverWindow;
    }

    public Variables getSMPConnectionContext() {
        return this.smpConnectionContext;
    }

    public void setSMPConnectionContext(Variables connectionContext) {
        this.smpConnectionContext = connectionContext;
    }
}
