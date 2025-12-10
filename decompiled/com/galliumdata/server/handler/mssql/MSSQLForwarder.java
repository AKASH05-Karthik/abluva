/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.ServerException;
import com.galliumdata.server.adapters.AdapterCallbackResponse;
import com.galliumdata.server.adapters.Variables;
import com.galliumdata.server.handler.PacketGroup;
import com.galliumdata.server.handler.mssql.ClientForwarder;
import com.galliumdata.server.handler.mssql.ConnectionState;
import com.galliumdata.server.handler.mssql.DataTypeWriter;
import com.galliumdata.server.handler.mssql.MSSQLAdapter;
import com.galliumdata.server.handler.mssql.MSSQLPacket;
import com.galliumdata.server.handler.mssql.MSSQLUtils;
import com.galliumdata.server.handler.mssql.PacketReader;
import com.galliumdata.server.handler.mssql.PreLoginPacket;
import com.galliumdata.server.handler.mssql.RawPacket;
import com.galliumdata.server.handler.mssql.RawPacketGroup;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.SMPSession;
import com.galliumdata.server.handler.mssql.UnableToParseException;
import com.galliumdata.server.handler.mssql.tokens.MessageToken;
import com.galliumdata.server.handler.mssql.tokens.TokenRowBatch;
import com.galliumdata.server.log.Markers;
import com.galliumdata.server.repository.FilterStage;
import com.galliumdata.server.util.BinaryDump;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class MSSQLForwarder
implements Runnable {
    private MSSQLForwarder otherForwarder;
    protected PacketReader reader;
    protected Socket inSock;
    protected Socket outSock;
    protected OutputStream out;
    protected MSSQLAdapter adapter;
    protected MSSQLPacket lastPacket;
    protected boolean stopRequested = false;
    protected Variables connectionContext;
    protected static ThreadLocal<Variables> threadContext = new ThreadLocal();
    protected Thread thread;
    protected ConnectionState connectionState;
    public static final String CONN_CTXT_PRELOGIN_REQUEST = "CONN_CTXT_PRELOGIN_REQUEST";
    public static final String CONN_CTXT_PRELOGIN_RESPONSE = "CONN_CTXT_PRELOGIN_RESPONSE";
    public static final String CONN_CTXT_LOGIN_PACKET = "CONN_CTXT_LOGIN_PACKET";
    public static final String CONN_CTXT_VAR_USERNAME = "userName";
    public static final String CONN_CTXT_VAR_USER_IP = "userIP";
    public static final String CONN_CTXT_VAR_LAST_SQL = "lastSQL";
    public static final String CONN_CTXT_VAR_CLIENT_FORWARDER = "clientForwarder";
    public static final String CONN_CTXT_VAR_SERVER_FORWARDER = "serverForwarder";
    protected SSLEngine sslEngine;
    protected byte[] networkReadBuffer;
    protected ByteBuffer networkReadBytes;
    protected byte[] networkWriteBuffer;
    protected ByteBuffer networkWriteBytes;
    protected ByteBuffer sslAppBytes;
    protected boolean sslAvailable;
    protected boolean inSslMode;
    protected PreLoginPacket preLoginPacket;
    private static final Logger log = LogManager.getLogger((String)"galliumdata.dbproto");
    protected static final Logger sslLog = LogManager.getLogger((String)"galliumdata.ssl");
    protected static final Logger logNet = LogManager.getLogger((String)"galliumdata.network");
    public static final boolean SAVE_PACKETS = false;

    public MSSQLForwarder(Socket inSock, Socket outSock, Variables connectionContext, ConnectionState connectionState) {
        this.connectionContext = connectionContext;
        this.connectionState = connectionState;
        this.inSock = inSock;
        try {
            this.reader = new PacketReader(inSock, this.toString(), this);
            if (outSock != null) {
                this.out = new BufferedOutputStream(outSock.getOutputStream());
                this.outSock = outSock;
            }
        }
        catch (IOException ioex) {
            throw new RuntimeException(ioex);
        }
    }

    @Override
    public void run() {
        while (!this.stopRequested) {
            RawPacket rawPacket = null;
            try {
                rawPacket = this.reader.readNextPacket();
            }
            catch (Exception ex) {
                log.debug(Markers.MSSQL, "Exception while reading packet: {}", (Object)ex.getMessage());
            }
            if (rawPacket == null) {
                try {
                    this.out.close();
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
                this.otherForwarder.requestStop();
                return;
            }
            RawPacketGroup pktGrp = new RawPacketGroup(rawPacket);
            if (!(rawPacket.getPacketId() != 1 || rawPacket.isEndOfMessage() || rawPacket.getBuffer()[0] != 1 && rawPacket.getBuffer()[0] != 3)) {
                RawPacket moreRawPkt = rawPacket;
                while (!moreRawPkt.isEndOfMessage()) {
                    moreRawPkt = this.reader.readNextPacket();
                    rawPacket.addBytes(moreRawPkt.getBuffer(), 8);
                }
            }
            try {
                this.processPacket(pktGrp);
            }
            catch (UnableToParseException utpex) {
                log.debug(Markers.MSSQL, "Unable to parse packet, forwarding it blindly, cause: {}", (Object)utpex.getMessage());
            }
            catch (Exception ex) {
                log.debug(Markers.MSSQL, "Exception while processing packet, closing connection, error was: {}", (Object)ex.getMessage());
                try {
                    this.out.close();
                }
                catch (Exception ex2) {
                    ex2.printStackTrace();
                }
                this.getOtherForwarder().requestStop();
                return;
            }
            this.writePacketGroup(pktGrp);
        }
        this.cleanup();
    }

    protected abstract void processPacket(RawPacketGroup var1);

    public MSSQLForwarder getOtherForwarder() {
        return this.otherForwarder;
    }

    public void setOtherForwarder(MSSQLForwarder otherForwarder) {
        this.otherForwarder = otherForwarder;
    }

    public void requestStop() {
        this.stopRequested = true;
        try {
            this.inSock.close();
        }
        catch (Exception ex) {
            log.trace(Markers.MSSQL, "Exception caught when closing inSock in requestStop for {}: {}", (Object)this, (Object)ex.getMessage());
        }
        this.getOtherForwarder().reader.close();
    }

    protected void writePacketGroup(RawPacketGroup pktGroup) {
        if (pktGroup.getSize() == 0) {
            return;
        }
        int totalSize = 0;
        for (RawPacket pkt : pktGroup.getPackets()) {
            totalSize += pkt.getBuffer().length;
        }
        byte[] buffer = new byte[totalSize];
        int idx = 0;
        for (RawPacket pkt : pktGroup.getPackets()) {
            System.arraycopy(pkt.getBuffer(), 0, buffer, idx, pkt.getBuffer().length);
            idx += pkt.getBuffer().length;
        }
        try {
            this.writeOut(buffer, 0, buffer.length);
        }
        catch (IOException ioex) {
            throw new RuntimeException(ioex);
        }
    }

    protected void writeOutWithSMP(byte[] buffer, int offset, int length, int smpSessionId) throws IOException {
        buffer[offset] = 83;
        buffer[offset + 1] = 8;
        DataTypeWriter.encodeTwoByteIntegerLow(buffer, offset + 2, (short)smpSessionId);
        DataTypeWriter.encodeFourByteIntegerLow(buffer, offset + 4, length);
        this.writeOut(buffer, 0, buffer.length);
    }

    protected void writeOut(byte[] buffer, int offset, int length) throws IOException {
        this.logNetwork(buffer, offset, length);
        if (this.inSslMode) {
            if (sslLog.isTraceEnabled()) {
                sslLog.trace(Markers.MSSQL, this.toString() + " is about to encrypt and send:\n" + BinaryDump.getBinaryDump(buffer, offset, length));
            }
            SSLEngine engine = this.getOtherForwarder().sslEngine;
            byte[] clearBytes = new byte[length];
            System.arraycopy(buffer, offset, clearBytes, 0, length);
            ByteBuffer clearBuffer = ByteBuffer.wrap(clearBytes);
            byte[] encryptedBytes = new byte[engine.getSession().getPacketBufferSize()];
            ByteBuffer encryptedBuffer = ByteBuffer.wrap(encryptedBytes);
            encryptedBuffer.clear();
            try {
                SSLEngineResult sslResult = engine.wrap(clearBuffer, encryptedBuffer);
                if (sslResult.bytesProduced() == 0) {
                    throw new ServerException("db.mssql.ssl.DecryptionError", "No bytes produced");
                }
                if (sslResult.getStatus() != SSLEngineResult.Status.OK) {
                    throw new ServerException("db.mssql.ssl.DecryptionError", "Status: " + String.valueOf((Object)sslResult.getStatus()));
                }
                this.out.write(encryptedBytes, 0, sslResult.bytesProduced());
                this.out.flush();
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            return;
        }
        if (log.isTraceEnabled()) {
            log.trace(Markers.MSSQL, String.valueOf(this) + " is about to send:\n" + BinaryDump.getBinaryDump(buffer, offset, length));
        }
        this.out.write(buffer, offset, length);
        this.out.flush();
    }

    protected RawPacket encryptPacket(MSSQLPacket pkt) {
        RawPacketWriter writer = new RawPacketWriter(this.connectionState, pkt, this);
        pkt.write(writer);
        writer.finalizePacket();
        RawPacket rawPkt = writer.getPacket();
        ByteBuffer pktBuf = ByteBuffer.wrap(rawPkt.getWrittenBuffer());
        byte[] writeBuf = new byte[32768];
        ByteBuffer byteBuf = ByteBuffer.wrap(writeBuf);
        try {
            SSLEngineResult result = this.getOtherForwarder().sslEngine.wrap(pktBuf, byteBuf);
            byte[] finalBuf = new byte[result.bytesProduced()];
            System.arraycopy(writeBuf, 0, finalBuf, 0, result.bytesProduced());
            return new RawPacket(finalBuf);
        }
        catch (Exception ex) {
            throw new ServerException("db.mssql.ssl.EncryptionException", String.valueOf(this.getClass()) + ".encryptPacket", ex);
        }
    }

    protected void cleanup() {
    }

    public ConnectionState getConnectionState() {
        return this.connectionState;
    }

    public abstract String getReceiveFromName();

    public abstract String getForwardToName();

    public static Variables getThreadContext() {
        Variables var = threadContext.get();
        if (var == null) {
            var = new Variables();
            threadContext.set(var);
        }
        return var;
    }

    protected AdapterCallbackResponse callRequestFilters(MSSQLPacket packet, RawPacketGroup rawPackets) {
        if (!this.adapter.getCallbackAdapter().hasFiltersForPacketType(FilterStage.REQUEST, packet.getPacketType())) {
            return new AdapterCallbackResponse();
        }
        Variables context = new Variables();
        context.put("packet", packet);
        context.put("packets", rawPackets);
        context.put("clientAddress", this.inSock.getInetAddress());
        context.put("mssqlutils", new MSSQLUtils((ClientForwarder)this));
        if (packet.isWrappedInSMP()) {
            int sid = packet.getSMPSessionId();
            SMPSession smpSession = this.connectionState.getSMPSession((short)sid);
            if (smpSession == null) {
                throw new ServerException("db.mssql.protocol.InvalidSMPSession", sid);
            }
            context.put("connectionContext", smpSession.getSMPConnectionContext());
        } else {
            context.put("connectionContext", this.connectionContext);
        }
        context.put("threadContext", MSSQLForwarder.getThreadContext());
        PacketGroup responsePackets = new PacketGroup();
        context.put("responsePackets", responsePackets);
        AdapterCallbackResponse response = this.adapter.getCallbackAdapter().invokeRequestFilters(packet.getPacketType(), context);
        if (response.reject) {
            log.trace(Markers.MSSQL, "Event has been rejected by user logic: {}", (Object)response.errorMessage);
            return response;
        }
        if (responsePackets.isModified()) {
            log.trace(Markers.MSSQL, "User logic has provided the response");
            RawPacketWriter writer = new RawPacketWriter(this.connectionState, packet, this);
            for (int i = 0; i < responsePackets.getSize(); ++i) {
                ((MSSQLPacket)responsePackets.get(i)).write(writer);
            }
            writer.close();
        }
        return response;
    }

    protected AdapterCallbackResponse callRequestFilters(MSSQLPacket packet, PacketGroup<MessageToken> tokens) {
        if (!this.adapter.getCallbackAdapter().hasFiltersForPacketType(FilterStage.REQUEST, packet.getPacketType())) {
            return new AdapterCallbackResponse();
        }
        Variables context = new Variables();
        context.put("packet", packet);
        context.put("packets", tokens);
        context.put("clientAddress", this.inSock.getInetAddress());
        if (packet.isWrappedInSMP()) {
            int sid = packet.getSMPSessionId();
            SMPSession smpSession = this.connectionState.getSMPSession((short)sid);
            if (smpSession == null) {
                throw new ServerException("db.mssql.protocol.InvalidSMPSession", sid);
            }
            context.put("connectionContext", smpSession.getSMPConnectionContext());
        } else {
            context.put("connectionContext", this.connectionContext);
        }
        context.put("threadContext", MSSQLForwarder.getThreadContext());
        PacketGroup responseTokens = new PacketGroup();
        context.put("responsePackets", responseTokens);
        AdapterCallbackResponse response = this.adapter.getCallbackAdapter().invokeRequestFilters(packet.getPacketType(), context);
        if (response.reject) {
            log.trace(Markers.MSSQL, "Event has been rejected by user logic: {}", (Object)response.errorMessage);
            return response;
        }
        if (responseTokens.isModified()) {
            log.trace(Markers.MSSQL, "User logic has provided the response");
            RawPacketWriter writer = new RawPacketWriter(this.connectionState, packet, this);
            for (int i = 0; i < responseTokens.getSize(); ++i) {
                ((MessageToken)responseTokens.get(i)).write(writer);
            }
            writer.close();
        }
        return response;
    }

    protected AdapterCallbackResponse callResponseFilters(MSSQLPacket packet, RawPacketGroup pktGrp) {
        if (!this.adapter.getCallbackAdapter().hasFiltersForPacketType(FilterStage.RESPONSE, packet.getPacketType())) {
            return new AdapterCallbackResponse();
        }
        Variables context = new Variables();
        context.put("packet", packet);
        PacketGroup<MSSQLPacket> packets = new PacketGroup<MSSQLPacket>();
        packets.addPacketNoModify(packet);
        context.put("packets", packets);
        context.put("connectionContext", this.connectionContext);
        context.put("threadContext", MSSQLForwarder.getThreadContext());
        AdapterCallbackResponse response = this.adapter.getCallbackAdapter().invokeResponseFilters(packet.getPacketType(), context);
        if (response.reject) {
            log.trace(Markers.MSSQL, "Event has been rejected by user logic: {}", (Object)response.errorMessage);
            return response;
        }
        if (packet.isRemoved()) {
            packets.removePacket(packet);
        }
        if (packets.isModified()) {
            RawPacketWriter writer = new RawPacketWriter(this.connectionState, packet, this);
            for (int i = 0; i < packets.getSize(); ++i) {
                MSSQLPacket pkt = (MSSQLPacket)packets.get(i);
                if (pkt.isRemoved()) continue;
                pkt.write(writer);
            }
        }
        return response;
    }

    protected AdapterCallbackResponse callResponseFilters(PacketGroup<MessageToken> tokens) {
        MessageToken token = tokens.get(0);
        if (!this.adapter.getCallbackAdapter().hasFiltersForPacketType(FilterStage.RESPONSE, token.getPacketType())) {
            return new AdapterCallbackResponse();
        }
        Variables context = new Variables();
        context.put("packet", token);
        context.put("packets", tokens);
        context.put("connectionContext", this.connectionContext);
        context.put("threadContext", MSSQLForwarder.getThreadContext());
        AdapterCallbackResponse response = this.adapter.getCallbackAdapter().invokeResponseFilters(token.getPacketType(), context);
        if (response.reject) {
            log.trace(Markers.MSSQL, "Event has been rejected by user logic: {}", (Object)response.errorMessage);
            return response;
        }
        if (token.isRemoved()) {
            tokens.removePacket(token);
        }
        return response;
    }

    protected AdapterCallbackResponse callResponseFiltersForBatch(TokenRowBatch batch) {
        if (!this.adapter.getCallbackAdapter().hasFiltersForPacketType(FilterStage.RESPONSE, batch.getPacketType())) {
            return new AdapterCallbackResponse();
        }
        Variables context = new Variables();
        context.put("packet", batch);
        context.put("packets", batch);
        context.put("connectionContext", this.connectionContext);
        context.put("threadContext", MSSQLForwarder.getThreadContext());
        AdapterCallbackResponse response = this.adapter.getCallbackAdapter().invokeResponseFilters(batch.getPacketType(), context);
        if (response.reject) {
            log.trace(Markers.MSSQL, "Event has been rejected by user logic: {}", (Object)response.errorMessage);
            return response;
        }
        return response;
    }

    protected MSSQLAdapter getAdapter() {
        return this.adapter;
    }

    protected void setAdapter(MSSQLAdapter adapter) {
        this.adapter = adapter;
    }

    public PreLoginPacket getFirstPreLoginPacket() {
        return this.preLoginPacket;
    }

    protected abstract void incrementBytesReceived(long var1);

    protected void logNetwork(byte[] bytes, int idx, int len) {
        if (!logNet.isDebugEnabled()) {
            return;
        }
        if (logNet.isTraceEnabled()) {
            String bytesStr = BinaryDump.getBinaryDump(bytes, idx, len);
            logNet.trace(Markers.MSSQL, "Connection " + this.adapter.getConnection().getName() + " to " + this.getForwardToName() + ":\n" + bytesStr);
        }
    }

    public String toString() {
        return "Generic MSSQL forwarder";
    }
}
