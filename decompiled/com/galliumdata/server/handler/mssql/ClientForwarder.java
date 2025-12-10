/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.logging.log4j.Level
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.ServerException;
import com.galliumdata.server.adapters.AdapterCallbackResponse;
import com.galliumdata.server.adapters.Variables;
import com.galliumdata.server.handler.PacketGroup;
import com.galliumdata.server.handler.mssql.AttentionPacket;
import com.galliumdata.server.handler.mssql.BulkLoadBCPPacket;
import com.galliumdata.server.handler.mssql.ConnectionState;
import com.galliumdata.server.handler.mssql.DataTypeWriter;
import com.galliumdata.server.handler.mssql.FederatedAuthenticationPacket;
import com.galliumdata.server.handler.mssql.Login7Packet;
import com.galliumdata.server.handler.mssql.MSSQLForwarder;
import com.galliumdata.server.handler.mssql.MSSQLPacket;
import com.galliumdata.server.handler.mssql.MSSQLResultSet;
import com.galliumdata.server.handler.mssql.MessagePacket;
import com.galliumdata.server.handler.mssql.PreLoginPacket;
import com.galliumdata.server.handler.mssql.RPCPacket;
import com.galliumdata.server.handler.mssql.RawPacket;
import com.galliumdata.server.handler.mssql.RawPacketGroup;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.SMPPacket;
import com.galliumdata.server.handler.mssql.SMPSession;
import com.galliumdata.server.handler.mssql.SQLBatchPacket;
import com.galliumdata.server.handler.mssql.SSPIMessagePacket;
import com.galliumdata.server.handler.mssql.SimplePacketReader;
import com.galliumdata.server.handler.mssql.TransactionManagerRequest;
import com.galliumdata.server.handler.mssql.UnknownPacket;
import com.galliumdata.server.handler.mssql.tokens.MessageToken;
import com.galliumdata.server.handler.mssql.tokens.TokenColMetadata;
import com.galliumdata.server.handler.mssql.tokens.TokenDone;
import com.galliumdata.server.handler.mssql.tokens.TokenError;
import com.galliumdata.server.handler.mssql.tokens.TokenRow;
import com.galliumdata.server.log.Markers;
import com.galliumdata.server.util.BinaryDump;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientForwarder
extends MSSQLForwarder {
    private static final Pattern SP_UNPREPARE_PATTERN = Pattern.compile("exec\\s+sp_unprepare\\s+(\\d+)", 10);
    private static final Logger log = LogManager.getLogger((String)"galliumdata.dbproto");

    public ClientForwarder(Socket inSock, Socket outSock, Variables connectionContext, ConnectionState connectionState) {
        super(inSock, outSock, connectionContext, connectionState);
    }

    @Override
    protected void processPacket(RawPacketGroup pktGrp) {
        MSSQLPacket pkt;
        RawPacket rawPacket = pktGrp.getPackets().get(0);
        this.adapter.getStatus().incrementNumRequests(1L);
        switch (rawPacket.getTypeCode()) {
            case 1: {
                pkt = new SQLBatchPacket(this.connectionState);
                pkt.setSMPSessionId(rawPacket.getSMPSessionId());
                break;
            }
            case 3: {
                pkt = new RPCPacket(this.connectionState);
                pkt.setSMPSessionId(rawPacket.getSMPSessionId());
                break;
            }
            case 6: {
                pkt = new AttentionPacket(this.connectionState);
                pkt.setSMPSessionId(rawPacket.getSMPSessionId());
                break;
            }
            case 7: {
                pkt = new BulkLoadBCPPacket(this.connectionState);
                break;
            }
            case 8: {
                pkt = new FederatedAuthenticationPacket(this.connectionState);
                break;
            }
            case 14: {
                pkt = new TransactionManagerRequest(this.connectionState);
                pkt.setSMPSessionId(rawPacket.getSMPSessionId());
                break;
            }
            case 16: {
                pkt = new Login7Packet(this.connectionState);
                pkt.setSMPSessionId(rawPacket.getSMPSessionId());
                if (this.preLoginPacket != null) break;
                this.preLoginPacket = new PreLoginPacket(this.connectionState);
                if (this.getOtherForwarder().preLoginPacket != null) break;
                this.getOtherForwarder().preLoginPacket = this.preLoginPacket;
                break;
            }
            case 17: {
                pkt = new SSPIMessagePacket(this.connectionState);
                break;
            }
            case 18: {
                pkt = new PreLoginPacket(this.connectionState, this.preLoginPacket != null);
                break;
            }
            case 23: {
                ByteBuffer byteBuffer = ByteBuffer.wrap(rawPacket.getBuffer());
                ByteBuffer decryptOut = ByteBuffer.allocate(rawPacket.getBuffer().length);
                SSLEngineResult sslResult = null;
                try {
                    sslResult = this.sslEngine.unwrap(byteBuffer, decryptOut);
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                    throw new RuntimeException(ex);
                }
                byte[] decryptedBytes = new byte[sslResult.bytesProduced()];
                System.arraycopy(decryptOut.array(), 0, decryptedBytes, 0, sslResult.bytesProduced());
                RawPacket decryptedRawPacket = new RawPacket(decryptedBytes);
                pktGrp.clear();
                pktGrp.addRawPacket(decryptedRawPacket);
                if (log.isTraceEnabled()) {
                    log.trace(Markers.MSSQL, "Received from client, decrypted:\n" + BinaryDump.getBinaryDump(decryptedBytes, 0, decryptedBytes.length));
                }
                this.inSslMode = true;
                boolean isLastPacket = decryptedRawPacket.isEndOfMessage();
                while (!isLastPacket) {
                    RawPacket moreRawPkt = this.reader.readNextPacket();
                    byteBuffer = ByteBuffer.wrap(moreRawPkt.getBuffer());
                    decryptOut = ByteBuffer.allocate(moreRawPkt.getBuffer().length);
                    try {
                        sslResult = this.sslEngine.unwrap(byteBuffer, decryptOut);
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                        throw new RuntimeException(ex);
                    }
                    decryptedBytes = new byte[sslResult.bytesProduced()];
                    System.arraycopy(decryptOut.array(), 0, decryptedBytes, 0, sslResult.bytesProduced());
                    decryptedRawPacket.addBytes(decryptedBytes, 8);
                    pktGrp.clear();
                    pktGrp.addRawPacket(decryptedRawPacket);
                    isLastPacket = (decryptedBytes[1] & 1) != 0;
                }
                decryptedRawPacket.setEndOfMessage(true);
                this.processPacket(pktGrp);
                return;
            }
            case 83: {
                SMPPacket smpPacket = new SMPPacket(this.connectionState);
                smpPacket.readFromBytes(rawPacket.getBuffer(), 0, rawPacket.getBuffer().length);
                switch (smpPacket.getSMPPacketType()) {
                    case SYN: {
                        this.connectionState.startSMPSession(smpPacket, this);
                        break;
                    }
                    case ACK: {
                        SMPSession smpSession = this.connectionState.getSMPSession((short)smpPacket.getSMPSessionId());
                        if (smpSession == null) {
                            throw new ServerException("db.mssql.protocol.InvalidSMPSession", smpPacket.getSMPSessionId());
                        }
                        smpSession.setClientWindow(smpPacket.getSMPWindow());
                        break;
                    }
                    case FIN: {
                        this.connectionState.closeSMPSession(smpPacket);
                        break;
                    }
                    case DATA: {
                        RawPacket unwrappedPacket = new RawPacket(smpPacket.getPayload());
                        unwrappedPacket.setSMPSessionId(smpPacket.getSMPSessionId());
                        SMPSession smpSession = this.connectionState.getSMPSession((short)smpPacket.getSMPSessionId());
                        if (smpSession == null) {
                            throw new ServerException("db.mssql.protocol.InvalidSMPSession", smpPacket.getSMPSessionId());
                        }
                        smpSession.setClientWindow(smpSession.getClientSeqNum() + (smpPacket.getSMPWindow() - smpPacket.getSeqNum()));
                        pktGrp.clear();
                        pktGrp.addRawPacket(unwrappedPacket);
                        this.processPacket(pktGrp);
                        return;
                    }
                }
                byte[] smpBytes = smpPacket.serialize();
                if (log.isTraceEnabled()) {
                    log.trace(Markers.MSSQL, "Forwarding SMP packet from client: " + String.valueOf(smpPacket));
                }
                try {
                    this.writeOut(smpBytes, 0, smpBytes.length);
                }
                catch (Exception ex) {
                    throw new ServerException("db.mssql.protocol.ErrorSendingPacket", ex.getMessage());
                }
                pktGrp.clear();
                return;
            }
            default: {
                pkt = new UnknownPacket(this.connectionState);
            }
        }
        pkt.readFromRawPacket(rawPacket);
        if (this.preLoginPacket == null && pkt instanceof PreLoginPacket) {
            this.preLoginPacket = (PreLoginPacket)pkt;
            this.connectionContext.put("CONN_CTXT_PRELOGIN_REQUEST", this.preLoginPacket);
        } else if (pkt.getTypeCode() == 18) {
            this.adapter.intializeClientSSLContext();
            this.startSSL(((PreLoginPacket)pkt).getSslData());
            RawPacket loginReqRaw = this.reader.readNextPacketPlain(true);
            if (loginReqRaw == null) {
                log.debug(Markers.MSSQL, "Login request not received because socket was closed");
                return;
            }
            ByteBuffer buf = ByteBuffer.wrap(loginReqRaw.getBuffer());
            byte[] outBufArray = new byte[32768];
            ByteBuffer outBuf = ByteBuffer.wrap(outBufArray);
            try {
                this.sslEngine.unwrap(buf, outBuf);
            }
            catch (Exception ex) {
                ex.printStackTrace();
                return;
            }
            pkt = new Login7Packet(this.connectionState);
            pkt.readFromBytes(outBufArray, 0, outBuf.position());
            ((Login7Packet)pkt).setServerName(this.connectionState.getServerName());
            pktGrp.clear();
            pktGrp.addPacket(pkt);
            this.connectionContext.put("CONN_CTXT_LOGIN_PACKET", pkt);
            String username = ((Login7Packet)pkt).getUsername();
            if (username != null && username.length() > 0) {
                this.connectionContext.put("userName", ((Login7Packet)pkt).getUsername());
            }
        }
        if (pkt instanceof SQLBatchPacket || pkt instanceof RPCPacket) {
            this.getUserName(pkt);
        }
        if (pkt instanceof SQLBatchPacket) {
            this.connectionContext.remove("lastSQL");
            SQLBatchPacket batch = (SQLBatchPacket)pkt;
            if (pkt.isWrappedInSMP()) {
                int sid = pkt.getSMPSessionId();
                SMPSession smpSession = this.connectionState.getSMPSession((short)sid);
                if (smpSession == null) {
                    throw new ServerException("db.mssql.protocol.InvalidSMPSession", sid);
                }
                smpSession.getSMPConnectionContext().put("lastSQL", batch.getSql());
            } else {
                this.connectionContext.put("lastSQL", batch.getSql());
            }
            String sql = batch.getSql();
            Matcher matcher = SP_UNPREPARE_PATTERN.matcher(sql);
            while (matcher.find()) {
                int id = Integer.parseInt(matcher.group(1));
                this.connectionState.closePreparedStatement(id);
            }
        }
        this.connectionState.setLastPacketFromClient(pkt);
        AdapterCallbackResponse resp = this.callRequestFilters(pkt, pktGrp);
        if (resp.reject) {
            log.debug(Markers.MSSQL, "Request filter '" + resp.logicName + "' has rejected request: " + resp.errorMessage);
            if (resp.errorResponse != null) {
                log.debug(Markers.MSSQL, "Sending custom response provided by request filter '" + resp.logicName + "'");
                try {
                    this.getOtherForwarder().writeOut(resp.errorResponse, 0, resp.errorResponse.length);
                }
                catch (IOException ex) {
                    log.error(Markers.MSSQL, "Exception while sending back rejection packet: " + ex.getMessage());
                }
            }
            pktGrp.clear();
            if (resp.errorMessage != null) {
                MessagePacket errMsgPkt = new MessagePacket(this.connectionState);
                errMsgPkt.setSMPSessionId(rawPacket.getSMPSessionId());
                TokenError errorToken = new TokenError(this.connectionState);
                errorToken.setMessage(resp.errorMessage);
                if (resp.errorCode != 0L) {
                    errorToken.setErrorNumber((int)resp.errorCode);
                }
                RawPacketWriter writer = new RawPacketWriter(this.connectionState, errMsgPkt, this.getOtherForwarder());
                errorToken.write(writer);
                TokenDone doneToken = new TokenDone(this.connectionState);
                doneToken.setDoneError(true);
                doneToken.setDoneFinal(true);
                doneToken.write(writer);
                writer.close();
                if (resp.closeConnection) {
                    log.debug(Markers.MSSQL, "Closing connection, as requested by filter '" + resp.logicName + "'");
                    this.requestStop();
                }
                return;
            }
            if (resp.closeConnection) {
                log.debug(Markers.MSSQL, "Closing connection, as requested by filter '" + resp.logicName + "'");
                this.requestStop();
            }
        }
        if (pkt instanceof SQLBatchPacket) {
            SQLBatchPacket batch = (SQLBatchPacket)pkt;
            if (pkt.isWrappedInSMP()) {
                int sid = pkt.getSMPSessionId();
                SMPSession smpSession = this.connectionState.getSMPSession((short)sid);
                if (smpSession == null) {
                    throw new ServerException("db.mssql.protocol.InvalidSMPSession", sid);
                }
                smpSession.getSMPConnectionContext().put("lastSQL", batch.getSql());
            } else {
                this.connectionContext.put("lastSQL", batch.getSql());
            }
        } else if (pkt instanceof BulkLoadBCPPacket) {
            BulkLoadBCPPacket msgPkt = (BulkLoadBCPPacket)pkt;
            RawPacketReader rawReader = new RawPacketReader(this.connectionState, this.reader);
            rawReader.setCurrentPacket(rawPacket, 0);
            pkt.read(rawReader);
            RawPacketWriter rawWriter = new RawPacketWriter(this.connectionState, msgPkt, (MSSQLForwarder)this);
            MessageToken token = msgPkt.readNextToken(rawReader);
            while (token != null) {
                PacketGroup<MessageToken> tokens = new PacketGroup<MessageToken>();
                tokens.addPacket(token);
                AdapterCallbackResponse response = this.callRequestFilters(pkt, tokens);
                for (int i = 0; i < tokens.getSize(); ++i) {
                    MessageToken tok = tokens.get(i);
                    if (tok.isRemoved()) continue;
                    tok.write(rawWriter);
                }
                token = msgPkt.readNextToken(rawReader);
                if (token != null) continue;
                break;
            }
            rawWriter.close();
            pktGrp.clear();
            return;
        }
        if (pkt instanceof Login7Packet && this.sslAvailable) {
            pktGrp.clear();
            pktGrp.addRawPacket(this.encryptPacket(pkt));
        } else {
            RawPacketWriter writer = new RawPacketWriter(this.connectionState, pkt, (MSSQLForwarder)this);
            pkt.write(writer);
            writer.close();
            pktGrp.clear();
        }
    }

    private void getUserName(MSSQLPacket pkt) {
        if (!this.connectionContext.hasMember("userName")) {
            String username = this.queryForOneString(pkt, "select system_user as username", "username");
            if (username == null || username.isBlank()) {
                this.connectionContext.put("userName", "<unknown>");
            } else {
                this.connectionContext.put("userName", username);
            }
        }
    }

    private void readFullEncryptedPacket(RawPacket pkt) {
        RawPacket moreRawPkt = pkt;
        while (!moreRawPkt.isEndOfMessage()) {
            SSLEngineResult sslResult;
            moreRawPkt = this.reader.readNextPacket();
            ByteBuffer byteBuffer = ByteBuffer.wrap(moreRawPkt.getBuffer());
            ByteBuffer decryptOut = ByteBuffer.allocate(moreRawPkt.getBuffer().length);
            try {
                sslResult = this.sslEngine.unwrap(byteBuffer, decryptOut);
            }
            catch (Exception ex) {
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }
            byte[] decryptedBytes = new byte[sslResult.bytesProduced()];
            System.arraycopy(decryptOut.array(), 0, decryptedBytes, 0, sslResult.bytesProduced());
            pkt.addBytes(decryptedBytes, 8);
        }
    }

    @Override
    protected void writeOutWithSMP(byte[] buffer, int offset, int length, int smpSessionId) throws IOException {
        byte[] smpBuffer = new byte[length + 16];
        SMPSession smpSession = this.connectionState.getSMPSession((short)smpSessionId);
        DataTypeWriter.encodeFourByteIntegerLow(smpBuffer, 8, smpSession.getAndIncrementClientSeqNum());
        DataTypeWriter.encodeFourByteIntegerLow(smpBuffer, 12, smpSession.getClientWindow());
        System.arraycopy(buffer, offset, smpBuffer, 16, length);
        super.writeOutWithSMP(smpBuffer, 0, smpBuffer.length, smpSessionId);
    }

    @Override
    protected void incrementBytesReceived(long num) {
        this.adapter.getStatus().incrementNumRequestBytes(num);
    }

    @Override
    public String getReceiveFromName() {
        return "client";
    }

    @Override
    public String getForwardToName() {
        return "server";
    }

    @Override
    public String toString() {
        return "MSSQL client forwarder";
    }

    private String queryForOneString(MSSQLPacket model, String sql, String columnName) {
        MSSQLResultSet rs = this.queryForRows(model, sql);
        TokenRow row = rs.getRows().get(0);
        return (String)row.getValue(columnName);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public MSSQLResultSet queryForRows(MSSQLPacket model, String sql) {
        MessageToken dataToken;
        Object signal;
        SQLBatchPacket userQuery = new SQLBatchPacket(this.connectionState);
        userQuery.setStatusEndOfMessage(true);
        if (model instanceof SQLBatchPacket) {
            userQuery.copyStreamHeadersFrom((SQLBatchPacket)model);
        } else {
            userQuery.setPacketId(model.getPacketId());
            userQuery.setSpid(model.getSpid());
            userQuery.setWindow(model.getWindow());
            userQuery.addStreamHeaders();
        }
        if (model.isWrappedInSMP()) {
            userQuery.setSMPSessionId(model.getSMPSessionId());
        }
        this.getOtherForwarder().reader.sideBandReadSignal = signal = new Object();
        userQuery.setSql(sql);
        RawPacketWriter writer = new RawPacketWriter(this.connectionState, userQuery, (MSSQLForwarder)this);
        userQuery.write(writer);
        Object object = signal;
        synchronized (object) {
            writer.close();
            try {
                signal.wait();
            }
            catch (InterruptedException ioex) {
                throw new RuntimeException(ioex);
            }
        }
        RawPacket resultPkt = this.getOtherForwarder().reader.sideBandPacket;
        if (resultPkt == null) {
            throw new RuntimeException("Got null packet from sideband read");
        }
        if (resultPkt.getTypeCode() == 23) {
            // empty if block
        }
        if (resultPkt.getTypeCode() != 4) {
            throw new ServerException("db.mssql.protocol.UnexpectedPacketType", resultPkt.getTypeCode(), 4);
        }
        MessagePacket msgPkt = new MessagePacket(this.connectionState);
        SimplePacketReader spr = new SimplePacketReader(this.connectionState, resultPkt);
        RawPacketReader rawReader = new RawPacketReader(this.connectionState, spr);
        rawReader.setCurrentPacket(resultPkt, 0);
        msgPkt.read(rawReader);
        MessageToken metaToken = msgPkt.readNextToken(rawReader);
        if (!metaToken.getTokenTypeName().equals("ColMetadata")) {
            if ("Error".equals(metaToken.getTokenTypeName())) {
                return new MSSQLResultSet((TokenError)metaToken);
            }
            throw new ServerException("db.mssql.protocol.InternalError", "Error in queryForRows: first token not meta");
        }
        TokenColMetadata meta = (TokenColMetadata)metaToken;
        ArrayList<TokenRow> rows = new ArrayList<TokenRow>();
        while ((dataToken = msgPkt.readNextToken(rawReader)) != null && !dataToken.getTokenTypeName().equals("Done")) {
            if (!dataToken.getTokenTypeName().equals("Row")) continue;
            TokenRow dataRow = (TokenRow)dataToken;
            rows.add(dataRow);
        }
        MSSQLResultSet rs = new MSSQLResultSet(meta, rows);
        return rs;
    }

    protected void startSSL(byte[] sslOpening) {
        SSLEngineResult.HandshakeStatus handshakeStatus;
        SSLSession sslSession;
        log.trace(Markers.MSSQL, "Beginning SSL handshake for client");
        SSLEngineResult sslResult = null;
        try {
            this.sslEngine = this.adapter.clientSSLContext.createSSLEngine();
            this.sslEngine.setUseClientMode(false);
            this.sslEngine.setEnabledProtocols(new String[]{"TLSv1.2"});
            sslSession = this.sslEngine.getSession();
            this.sslEngine.beginHandshake();
            handshakeStatus = this.sslEngine.getHandshakeStatus();
        }
        catch (SSLException sex) {
            throw new RuntimeException(sex);
        }
        this.networkReadBuffer = new byte[sslSession.getPacketBufferSize()];
        this.networkReadBytes = ByteBuffer.wrap(this.networkReadBuffer);
        this.networkReadBytes.put(sslOpening);
        this.networkReadBytes.flip();
        this.networkWriteBuffer = new byte[sslSession.getPacketBufferSize() * 5];
        this.networkWriteBytes = ByteBuffer.wrap(this.networkWriteBuffer);
        byte[] sslAppBuffer = new byte[sslSession.getApplicationBufferSize()];
        this.sslAppBytes = ByteBuffer.wrap(sslAppBuffer);
        ByteBuffer emptyBytes = ByteBuffer.allocate(0);
        block13: while (handshakeStatus != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING && handshakeStatus != SSLEngineResult.HandshakeStatus.FINISHED) {
            sslLog.trace(Markers.MSSQL, "SSL handshake: handshakeStatus={}", (Object)handshakeStatus);
            block4 : switch (handshakeStatus) {
                case NEED_TASK: {
                    Runnable task;
                    sslLog.trace(Markers.MSSQL, "SSL handshake: need to run some tasks...");
                    while ((task = this.sslEngine.getDelegatedTask()) != null) {
                        task.run();
                    }
                    sslLog.trace(Markers.MSSQL, "SSL handshake: tasks have been run");
                    sslResult = null;
                    break;
                }
                case NEED_WRAP: {
                    this.networkWriteBytes.clear();
                    try {
                        sslResult = this.sslEngine.wrap(emptyBytes, this.networkWriteBytes);
                    }
                    catch (SSLException sex) {
                        throw new RuntimeException(sex);
                    }
                    if (sslResult.bytesConsumed() == 0 && sslResult.bytesProduced() == 0) {
                        throw new RuntimeException("NEED_WRAP is stuck in a loop");
                    }
                    while (this.sslEngine.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
                        byte[] buf = new byte[sslSession.getPacketBufferSize()];
                        ByteBuffer bbuf = ByteBuffer.wrap(buf);
                        try {
                            sslResult = this.sslEngine.wrap(emptyBytes, bbuf);
                        }
                        catch (SSLException sex) {
                            throw new RuntimeException(sex);
                        }
                        bbuf.flip();
                        this.networkWriteBytes.put(bbuf);
                        if (sslResult.bytesConsumed() != 0 || sslResult.bytesProduced() != 0) continue;
                        throw new RuntimeException("NEED_WRAP is stuck in a loop");
                    }
                    this.writePreLoginSSLPacket();
                    break;
                }
                case NEED_UNWRAP: {
                    if (this.networkReadBytes.position() >= this.networkReadBytes.limit()) {
                        if (sslLog.isEnabled(Level.TRACE)) {
                            sslLog.trace(Markers.MSSQL, "UNWRAP: position is: " + this.networkReadBytes.position() + ",  limit is: " + this.networkReadBytes.limit());
                        }
                        if (!this.readNextPreLoginSSLPacket()) {
                            sslLog.trace(Markers.MSSQL, "Nothing more SSL to read from the network");
                            break block13;
                        }
                    } else if (sslLog.isTraceEnabled()) {
                        sslLog.trace(Markers.MSSQL, "UNWRAP: still some bytes in the buffer, position is: {},  limit is: {}", (Object)this.networkReadBytes.position(), (Object)this.networkReadBytes.limit());
                    }
                    if (sslLog.isEnabled(Level.TRACE)) {
                        sslLog.trace(Markers.MSSQL, "We have " + (this.networkReadBytes.limit() - this.networkReadBytes.position()) + " bytes in the buffer, about to unwrap");
                    }
                    int numBytesConsumed = 0;
                    while (numBytesConsumed < this.networkReadBytes.limit() && (sslResult == null || sslResult.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_UNWRAP)) {
                        if (sslLog.isTraceEnabled()) {
                            sslLog.trace(Markers.MSSQL, "About to UNWRAP, position is: {}, limit is: {}", (Object)this.networkReadBytes.position(), (Object)this.networkReadBytes.limit());
                        }
                        try {
                            sslResult = this.sslEngine.unwrap(this.networkReadBytes, emptyBytes);
                        }
                        catch (SSLException sex) {
                            throw new RuntimeException(sex);
                        }
                        if (sslResult.getStatus() == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
                            this.networkReadBytes.compact();
                            if (this.readNextPreLoginSSLPacket()) continue;
                            sslLog.trace(Markers.MSSQL, "No data received after an SSL handshake underflow??");
                            break block4;
                        }
                        if (sslResult.bytesConsumed() == 0) {
                            sslLog.trace(Markers.MSSQL, "UNWRAP has consumed zero, so done for now");
                            break block4;
                        }
                        numBytesConsumed += sslResult.bytesConsumed();
                        if (sslLog.isEnabled(Level.TRACE)) {
                            sslLog.trace(Markers.MSSQL, "unwrapping result: {}, numBytesConsumed: {}", (Object)sslResult.getHandshakeStatus(), (Object)numBytesConsumed);
                        }
                        this.networkReadBytes.compact();
                        this.networkReadBytes.flip();
                        if (!sslLog.isTraceEnabled()) continue;
                        sslLog.trace(Markers.MSSQL, "After compact, position is: {}, limit is: {}", (Object)this.networkReadBytes.position(), (Object)this.networkReadBytes.limit());
                    }
                    break;
                }
                default: {
                    throw new RuntimeException("Unexpected handshake status: " + String.valueOf((Object)handshakeStatus));
                }
            }
            handshakeStatus = this.sslEngine.getHandshakeStatus();
            if (!sslLog.isTraceEnabled()) continue;
            sslLog.trace(Markers.MSSQL, "SSL status is: {}", (Object)(sslResult == null ? "null" : sslResult.toString()));
        }
        this.reader.setSslEngine(this.sslEngine);
        int numLeftOver = this.networkReadBytes.limit() - this.networkReadBytes.position();
        sslLog.debug(Markers.MSSQL, "SSL handshake is done, there are {} bytes left over", (Object)numLeftOver);
    }

    private boolean readNextPreLoginSSLPacket() {
        sslLog.trace(Markers.MSSQL, "Reading next prelogin SSL packet (client)");
        RawPacket rawPkt = this.reader.readNextPacketPlain(true);
        if (rawPkt == null) {
            return false;
        }
        this.networkReadBytes.clear();
        if (rawPkt.getTypeCode() != 18) {
            this.networkReadBytes.put(rawPkt.getBuffer());
        } else {
            PreLoginPacket pkt = new PreLoginPacket(this.connectionState, true);
            pkt.readFromRawPacket(rawPkt);
            this.networkReadBytes.put(pkt.getSslData());
        }
        this.networkReadBytes.flip();
        return true;
    }

    private void writePreLoginSSLPacket() {
        this.networkWriteBytes.flip();
        byte[] buf = new byte[this.networkWriteBytes.limit()];
        this.networkWriteBytes.get(buf);
        PreLoginPacket pkt = new PreLoginPacket(this.connectionState, true);
        pkt.setTypeCode((byte)18);
        pkt.setStatusEndOfMessage(true);
        pkt.setSslData(buf);
        RawPacketWriter writer = new RawPacketWriter(this.connectionState, pkt, this.getOtherForwarder());
        pkt.write(writer);
        sslLog.trace(Markers.MSSQL, "Writing prelogin SSL packet (client)");
        writer.close();
    }
}
