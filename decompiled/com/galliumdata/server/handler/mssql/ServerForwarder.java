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
import com.galliumdata.server.handler.ProtocolException;
import com.galliumdata.server.handler.mssql.ConnectionState;
import com.galliumdata.server.handler.mssql.DataTypeWriter;
import com.galliumdata.server.handler.mssql.MSSQLForwarder;
import com.galliumdata.server.handler.mssql.MSSQLPacket;
import com.galliumdata.server.handler.mssql.MessagePacket;
import com.galliumdata.server.handler.mssql.PreLoginPacket;
import com.galliumdata.server.handler.mssql.RPCPacket;
import com.galliumdata.server.handler.mssql.RawPacket;
import com.galliumdata.server.handler.mssql.RawPacketGroup;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.SMPPacket;
import com.galliumdata.server.handler.mssql.SMPSession;
import com.galliumdata.server.handler.mssql.tokens.MessageToken;
import com.galliumdata.server.handler.mssql.tokens.TokenError;
import com.galliumdata.server.handler.mssql.tokens.TokenReturnValue;
import com.galliumdata.server.handler.mssql.tokens.TokenRow;
import com.galliumdata.server.handler.mssql.tokens.TokenRowBatch;
import com.galliumdata.server.log.Markers;
import com.galliumdata.server.util.BinaryDump;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServerForwarder
extends MSSQLForwarder {
    private RawPacket firstPacketOfGroup;
    protected TokenRowBatch currentRowBatch;
    private final RawPacketGroup currentPacketGroup = new RawPacketGroup();
    public static final boolean DEBUG_MODE = false;
    private static final Logger log = LogManager.getLogger((String)"galliumdata.dbproto");

    public ServerForwarder(Socket inSock, Socket outSock, Variables connectionContext, ConnectionState connectionState) {
        super(inSock, outSock, connectionContext, connectionState);
    }

    @Override
    protected void processPacket(RawPacketGroup pktGrp) {
        MSSQLPacket pkt;
        RawPacket rawPacket = pktGrp.getPackets().get(0);
        this.adapter.getStatus().incrementNumResponses(1L);
        switch (rawPacket.getTypeCode()) {
            case 4: {
                if (this.preLoginPacket == null) {
                    pkt = new PreLoginPacket(this.connectionState);
                    break;
                }
                pkt = new MessagePacket(this.connectionState);
                pkt.setSMPSessionId(rawPacket.getSMPSessionId());
                break;
            }
            case 18: {
                pkt = new PreLoginPacket(this.connectionState, true);
                break;
            }
            case 3: {
                pkt = new RPCPacket(this.connectionState);
                pkt.setSMPSessionId(rawPacket.getSMPSessionId());
                break;
            }
            case 23: {
                ByteBuffer byteBuffer = ByteBuffer.wrap(rawPacket.getBuffer());
                ByteBuffer decryptOut = ByteBuffer.allocate(rawPacket.getBuffer().length * 2);
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
                    log.trace(Markers.MSSQL, "Decrypted:\n" + BinaryDump.getBinaryDump(decryptedBytes, 0, decryptedBytes.length));
                }
                this.inSslMode = true;
                this.processPacket(pktGrp);
                return;
            }
            case 83: {
                SMPSession smpSession;
                SMPPacket smpPacket = new SMPPacket(this.connectionState);
                smpPacket.readFromBytes(rawPacket.getBuffer(), 0, rawPacket.getBuffer().length);
                if (smpPacket.getSMPPacketType() == SMPPacket.SMPPacketType.DATA) {
                    smpSession = this.connectionState.getSMPSession((short)smpPacket.getSMPSessionId());
                    if (smpSession == null) {
                        throw new ServerException("db.mssql.protocol.InvalidSMPSession", smpPacket.getSMPSessionId());
                    }
                    if (smpSession.getServerWindow() == 0) {
                        smpSession.setServerWindow(smpPacket.getSMPWindow());
                    }
                }
                switch (smpPacket.getSMPPacketType()) {
                    case SYN: {
                        this.connectionState.startSMPSession(smpPacket, this);
                        break;
                    }
                    case ACK: {
                        smpSession = this.connectionState.getSMPSession((short)smpPacket.getSMPSessionId());
                        smpSession.setServerWindow(smpPacket.getSMPWindow());
                        break;
                    }
                    case FIN: {
                        this.connectionState.closeSMPSession(smpPacket);
                        break;
                    }
                    case DATA: {
                        RawPacket unwrappedPacket = new RawPacket(smpPacket.getPayload());
                        unwrappedPacket.setSMPSessionId(smpPacket.getSMPSessionId());
                        smpSession = this.connectionState.getSMPSession((short)smpPacket.getSMPSessionId());
                        if (smpSession == null) {
                            throw new ServerException("db.mssql.protocol.InvalidSMPSession", smpPacket.getSMPSessionId());
                        }
                        smpSession.setServerWindow(smpSession.getServerSeqNum() + (smpPacket.getSMPWindow() - smpPacket.getSeqNum()));
                        pktGrp.clear();
                        pktGrp.addRawPacket(unwrappedPacket);
                        this.processPacket(pktGrp);
                        return;
                    }
                }
                byte[] smpBytes = smpPacket.serialize();
                if (log.isTraceEnabled()) {
                    log.trace(Markers.MSSQL, "Forwarding SMP packet from server: " + String.valueOf(smpPacket));
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
                throw new ProtocolException("db.mssql.protocol.UnknownPacketType", rawPacket.getTypeCode());
            }
        }
        if (pkt instanceof MessagePacket) {
            MessagePacket msgPkt = (MessagePacket)pkt;
            RawPacketReader rawReader = new RawPacketReader(this.connectionState, this.reader);
            rawReader.setCurrentPacket(rawPacket, 0);
            pkt.read(rawReader);
            RawPacketWriter rawWriter = new RawPacketWriter(this.connectionState, msgPkt, (MSSQLForwarder)this);
            MessageToken token = msgPkt.readNextToken(rawReader);
            while (token != null) {
                TokenReturnValue retVal;
                String lastSp;
                AdapterCallbackResponse response;
                if ((token.getTokenType() == -47 || token.getTokenType() == -46) && this.rowsShouldBeBatched()) {
                    TokenRowBatch batch = this.getRowBatch();
                    batch.addRow((TokenRow)token);
                    if (batch.batchIsFull()) {
                        response = this.callResponseFiltersForBatch(batch);
                        if (response.reject) {
                            pktGrp.clear();
                            batch.clear();
                            continue;
                        }
                        pktGrp.clear();
                        batch.writeOut(rawWriter);
                    }
                    if ((token = msgPkt.readNextToken(rawReader)) != null) continue;
                    PacketGroup tokens = new PacketGroup();
                    AdapterCallbackResponse response2 = this.callResponseFiltersForBatch(batch);
                    if (response2.reject) {
                        pktGrp.clear();
                        batch.clear();
                        continue;
                    }
                    pktGrp.clear();
                    batch.writeOut(rawWriter);
                    rawWriter.close();
                    continue;
                }
                if (this.currentRowBatch != null && this.currentRowBatch.getRows().size() > 0) {
                    AdapterCallbackResponse response3 = this.callResponseFiltersForBatch(this.currentRowBatch);
                    if (response3.closeConnection) {
                        log.debug(Markers.MSSQL, "Connection " + this.getAdapter().getConnection().getName() + " is being closed by filter, message: " + response3.errorMessage);
                        this.requestStop();
                        return;
                    }
                    if (response3.reject) {
                        pktGrp.clear();
                        this.currentRowBatch.clear();
                        continue;
                    }
                    pktGrp.clear();
                    this.currentRowBatch.writeOut(rawWriter);
                }
                PacketGroup<MessageToken> tokens = new PacketGroup<MessageToken>();
                tokens.addPacket(token);
                if (token.getTokenType() == -84 && ("Sp_Prepare".equalsIgnoreCase(lastSp = this.connectionState.getLastRPC()) || "Sp_PrepExec".equalsIgnoreCase(lastSp)) && (retVal = (TokenReturnValue)token).getParamOrdinal() == 0) {
                    int id = (Integer)retVal.getValue().getValue();
                    String currentSql = (String)this.connectionContext.get("lastSQL");
                    this.getConnectionState().openPreparedStatement(currentSql, id);
                }
                response = this.callResponseFilters(tokens);
                if (response.reject) {
                    this.sendErrorMessage(response.errorMessage, (int)response.errorCode);
                    if (response.closeConnection) {
                        this.requestStop();
                    }
                    return;
                }
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
        pkt.readFromRawPacket(rawPacket);
        this.currentPacketGroup.addRawPacket(rawPacket);
        if (this.firstPacketOfGroup == null) {
            this.firstPacketOfGroup = rawPacket;
        }
        if (!pkt.isStatusEndOfMessage()) {
            throw new ServerException("db.mssql.protocol.LastPacketNotEndOfMessage", new Object[0]);
        }
        if (this.preLoginPacket == null && pkt instanceof PreLoginPacket) {
            this.preLoginPacket = (PreLoginPacket)pkt;
            this.connectionState.setServerMajorVersion(this.preLoginPacket.getMajorVersion());
            this.connectionContext.put("CONN_CTXT_PRELOGIN_RESPONSE", pkt);
            byte encryptionMode = this.preLoginPacket.getEncryption();
            if (encryptionMode != 2) {
                this.startSSL();
                this.sslAvailable = true;
                this.getOtherForwarder().sslAvailable = true;
            }
        }
        AdapterCallbackResponse resp = this.callResponseFilters(pkt, pktGrp);
        if (resp.reject) {
            log.debug(Markers.MSSQL, "Response filter '" + resp.logicName + "' has rejected response: " + resp.errorMessage);
            if (resp.errorResponse != null) {
                log.debug(Markers.MSSQL, "Sending custom response provided by request filter '" + resp.logicName + "'");
                try {
                    this.writeOut(resp.errorResponse, 0, resp.errorResponse.length);
                }
                catch (IOException ex) {
                    log.error(Markers.MSSQL, "Exception while sending back rejection packet: " + ex.getMessage());
                }
            }
            pktGrp.clear();
            if (resp.closeConnection) {
                log.debug(Markers.MSSQL, "Closing connection, as requested by filter '" + resp.logicName + "'");
                this.requestStop();
            }
        }
        RawPacketWriter writer = new RawPacketWriter(this.connectionState, this.firstPacketOfGroup, (MSSQLForwarder)this);
        pkt.write(writer);
        writer.close();
        pktGrp.clear();
        this.currentPacketGroup.clear();
        this.firstPacketOfGroup = null;
    }

    private boolean rowsShouldBeBatched() {
        return this.adapter.batchSizeBytes > 0 || this.adapter.batchSizeRows > 0;
    }

    private TokenRowBatch getRowBatch() {
        if (this.currentRowBatch == null) {
            this.currentRowBatch = new TokenRowBatch(this.adapter.batchSizeRows, this.adapter.batchSizeBytes);
        }
        return this.currentRowBatch;
    }

    @Override
    protected void writeOutWithSMP(byte[] buffer, int offset, int length, int smpSessionId) throws IOException {
        byte[] smpBuffer = new byte[length + 16];
        SMPSession smpSession = this.connectionState.getSMPSession((short)smpSessionId);
        int seqNum = smpSession.getAndIncrementServerSeqNum();
        DataTypeWriter.encodeFourByteIntegerLow(smpBuffer, 8, seqNum);
        DataTypeWriter.encodeFourByteIntegerLow(smpBuffer, 12, smpSession.getServerWindow());
        System.arraycopy(buffer, offset, smpBuffer, 16, length);
        super.writeOutWithSMP(smpBuffer, 0, smpBuffer.length, smpSessionId);
    }

    public void sendErrorMessage(String errMsg, int errNo) {
        MessagePacket pkt = new MessagePacket(this.connectionState);
        pkt.setPacketId((byte)1);
        pkt.setStatusEndOfMessage(true);
        TokenError errToken = new TokenError(this.connectionState);
        if (errMsg == null) {
            errMsg = "Unspecified error";
        }
        errToken.setMessage(errMsg);
        errToken.setErrorNumber(errNo);
        RawPacketWriter writer = new RawPacketWriter(this.connectionState, pkt, (MSSQLForwarder)this);
        errToken.write(writer);
        writer.close();
    }

    @Override
    protected void incrementBytesReceived(long num) {
        this.adapter.getStatus().incrementNumResponseBytes(num);
    }

    @Override
    public String getReceiveFromName() {
        return "server";
    }

    @Override
    public String getForwardToName() {
        return "client";
    }

    @Override
    public String toString() {
        return "MSSQL server forwarder";
    }

    protected void startSSL() {
        SSLEngineResult.HandshakeStatus handshakeStatus;
        SSLSession sslSession;
        log.trace(Markers.MSSQL, "Beginning SSL handshake for server");
        this.adapter.initializeServerSSLContext(this.connectionState.getServerMajorVersion());
        SSLEngineResult sslResult = null;
        try {
            this.sslEngine = this.adapter.serverSSLContext.createSSLEngine();
            this.sslEngine.setUseClientMode(true);
            if (this.connectionState.getServerMajorVersion() == 11) {
                this.sslEngine.setEnabledProtocols(new String[]{"TLSv1.1", "TLSv1"});
            } else {
                this.sslEngine.setEnabledProtocols(new String[]{"TLSv1.2"});
            }
            sslSession = this.sslEngine.getSession();
            this.sslEngine.beginHandshake();
            handshakeStatus = this.sslEngine.getHandshakeStatus();
        }
        catch (SSLException sex) {
            throw new RuntimeException(sex);
        }
        this.networkReadBuffer = new byte[sslSession.getPacketBufferSize()];
        this.networkReadBytes = ByteBuffer.wrap(this.networkReadBuffer);
        this.networkReadBytes.flip();
        this.networkWriteBuffer = new byte[sslSession.getPacketBufferSize() * 5];
        this.networkWriteBytes = ByteBuffer.wrap(this.networkWriteBuffer);
        byte[] sslAppBuffer = new byte[sslSession.getApplicationBufferSize()];
        this.sslAppBytes = ByteBuffer.wrap(sslAppBuffer);
        ByteBuffer emptyBytes = ByteBuffer.allocate(0);
        block13: while (handshakeStatus != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING && handshakeStatus != SSLEngineResult.HandshakeStatus.FINISHED) {
            sslLog.trace(Markers.MSSQL, "Server-side SSL handshake: handshakeStatus={}", (Object)handshakeStatus);
            block4 : switch (handshakeStatus) {
                case NEED_TASK: {
                    Runnable task;
                    sslLog.trace(Markers.MSSQL, "Server-side SSL handshake: need to run some tasks...");
                    while ((task = this.sslEngine.getDelegatedTask()) != null) {
                        task.run();
                    }
                    sslLog.trace(Markers.MSSQL, "Server-side SSL handshake: tasks have been run");
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
                        throw new RuntimeException("Server-side NEED_WRAP is stuck in a loop");
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
                        throw new RuntimeException("Server-side NEED_WRAP is stuck in a loop");
                    }
                    this.writePreLoginSSLPacket();
                    break;
                }
                case NEED_UNWRAP: {
                    if (this.networkReadBytes.position() >= this.networkReadBytes.limit()) {
                        if (sslLog.isEnabled(Level.TRACE)) {
                            sslLog.trace(Markers.MSSQL, "Server-side UNWRAP: position is: " + this.networkReadBytes.position() + ",  limit is: " + this.networkReadBytes.limit());
                        }
                        if (!this.readNextPreLoginSSLPacket()) {
                            sslLog.trace(Markers.MSSQL, "Server-side Nothing more SSL to read from the network");
                            break block13;
                        }
                    } else if (sslLog.isTraceEnabled()) {
                        sslLog.trace(Markers.MSSQL, "Server-side UNWRAP: still some bytes in the buffer, position is: {},  limit is: {}", (Object)this.networkReadBytes.position(), (Object)this.networkReadBytes.limit());
                    }
                    if (sslLog.isEnabled(Level.TRACE)) {
                        sslLog.trace(Markers.MSSQL, "Server-side We have " + (this.networkReadBytes.limit() - this.networkReadBytes.position()) + " bytes in the buffer, about to unwrap");
                    }
                    int numBytesConsumed = 0;
                    while (numBytesConsumed < this.networkReadBytes.limit() && (sslResult == null || sslResult.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_UNWRAP)) {
                        if (sslLog.isTraceEnabled()) {
                            sslLog.trace(Markers.MSSQL, "Server-side About to UNWRAP, position is: {}, limit is: {}", (Object)this.networkReadBytes.position(), (Object)this.networkReadBytes.limit());
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
                            sslLog.trace(Markers.MSSQL, "Server-side No data received after an SSL handshake underflow??");
                            break block4;
                        }
                        if (sslResult.bytesConsumed() == 0) {
                            sslLog.trace(Markers.MSSQL, "Server-side UNWRAP has consumed zero, so done for now");
                            break block4;
                        }
                        numBytesConsumed += sslResult.bytesConsumed();
                        if (sslLog.isEnabled(Level.TRACE)) {
                            sslLog.trace(Markers.MSSQL, "Server-side unwrapping result: {}, numBytesConsumed: {}", (Object)sslResult.getHandshakeStatus(), (Object)numBytesConsumed);
                        }
                        this.networkReadBytes.compact();
                        this.networkReadBytes.flip();
                        if (!sslLog.isTraceEnabled()) continue;
                        sslLog.trace(Markers.MSSQL, "Server-side After compact, position is: {}, limit is: {}", (Object)this.networkReadBytes.position(), (Object)this.networkReadBytes.limit());
                    }
                    break;
                }
                default: {
                    throw new RuntimeException("Server-side: unexpected handshake status: " + String.valueOf((Object)handshakeStatus));
                }
            }
            handshakeStatus = this.sslEngine.getHandshakeStatus();
            if (!sslLog.isTraceEnabled()) continue;
            sslLog.trace(Markers.MSSQL, "Server-side SSL status is: {}", (Object)(sslResult == null ? "null" : sslResult.toString()));
        }
        this.reader.setSslEngine(this.sslEngine);
        int numLeftOver = this.networkReadBytes.limit() - this.networkReadBytes.position();
        sslLog.debug(Markers.MSSQL, "Server-side SSL handshake is done, there are {} bytes left over", (Object)numLeftOver);
    }

    private boolean readNextPreLoginSSLPacket() {
        PreLoginPacket pkt;
        sslLog.trace(Markers.MSSQL, "Reading next prelogin SSL packet (server)");
        RawPacket rawPkt = this.reader.readNextPacketPlain(true);
        if (rawPkt == null) {
            return false;
        }
        this.networkReadBytes.clear();
        if (rawPkt.getTypeCode() != 18) {
            this.networkReadBytes.put(rawPkt.getBuffer());
        } else {
            pkt = new PreLoginPacket(this.connectionState, true);
            pkt.readFromRawPacket(rawPkt);
            this.networkReadBytes.put(pkt.getSslData());
        }
        while (!rawPkt.isEndOfMessage()) {
            sslLog.trace("Incomplete pre-login TLS package, reading the rest");
            rawPkt = this.reader.readNextPacketPlain(true);
            if (rawPkt == null) {
                return false;
            }
            pkt = new PreLoginPacket(this.connectionState, true);
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
        RawPacketWriter writer = new RawPacketWriter(this.connectionState, pkt, (MSSQLForwarder)this);
        pkt.write(writer);
        writer.finalizePacket();
        RawPacket rawPkt = writer.getPacket();
        sslLog.trace(Markers.MSSQL, "Writing prelogin SSL packet (server)");
        try {
            this.inSock.getOutputStream().write(rawPkt.getBuffer());
            this.inSock.getOutputStream().flush();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
