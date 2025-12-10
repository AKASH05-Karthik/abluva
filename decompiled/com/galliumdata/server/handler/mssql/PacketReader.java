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
import com.galliumdata.server.handler.ProtocolException;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.MSSQLForwarder;
import com.galliumdata.server.handler.mssql.RawPacket;
import com.galliumdata.server.log.Markers;
import com.galliumdata.server.util.BinaryDump;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PacketReader {
    protected final Socket socket;
    private InputStream socketIn = null;
    private final MSSQLForwarder forwarder;
    private final String name;
    public static final int PKT_MAX = 32768;
    private byte[] buffer = new byte[32768];
    private int readOffset = 0;
    private boolean needToReadMore = true;
    private boolean bufferResized = false;
    public Object sideBandReadSignal;
    public RawPacket sideBandPacket;
    private SSLEngine sslEngine;
    private static final Logger log = LogManager.getLogger((String)"galliumdata.dbproto");
    protected static final Logger logNet = LogManager.getLogger((String)"galliumdata.network");

    public PacketReader(Socket socket, String name, MSSQLForwarder forwarder) {
        this.socket = socket;
        this.name = name;
        this.forwarder = forwarder;
        try {
            if (socket != null) {
                this.socketIn = new BufferedInputStream(socket.getInputStream());
            }
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public RawPacket readNextPacket() {
        RawPacket pkt = this.readNextPacketPlain(false);
        if (this.sideBandReadSignal != null) {
            if (pkt.getTypeCode() == 23) {
                ByteBuffer byteBuffer = ByteBuffer.wrap(pkt.getBuffer());
                ByteBuffer decryptOut = ByteBuffer.allocate(pkt.getBuffer().length * 2);
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
                pkt = new RawPacket(decryptedBytes);
            }
            this.sideBandPacket = pkt;
            Object object = this.sideBandReadSignal;
            synchronized (object) {
                this.sideBandReadSignal.notify();
            }
            this.sideBandReadSignal = null;
            pkt = this.readNextPacketPlain(false);
        }
        return pkt;
    }

    public RawPacket readNextPacketPlain(boolean expectSSL) {
        short packetLen;
        block27: {
            packetLen = 0;
            while (true) {
                int readThisTime = 0;
                if (this.needToReadMore) {
                    int numRead;
                    try {
                        if (log.isEnabled(Level.TRACE)) {
                            log.trace(Markers.MSSQL, this.forwarder.toString() + " is reading from network...");
                        }
                        numRead = this.socketIn.read(this.buffer, this.readOffset, this.buffer.length - this.readOffset);
                        if (log.isEnabled(Level.TRACE)) {
                            log.trace(Markers.MSSQL, this.forwarder.toString() + " has read from network:\n" + BinaryDump.getBinaryDump(this.buffer, this.readOffset, numRead));
                        }
                        readThisTime = numRead;
                        this.forwarder.incrementBytesReceived(numRead);
                        this.logNetwork(this.buffer, this.readOffset, numRead);
                    }
                    catch (SocketException sockex) {
                        if ("Socket closed".equals(sockex.getMessage())) {
                            log.trace(Markers.MSSQL, "Socket has been closed: port " + this.socket.getPort() + ", local port: " + this.socket.getLocalPort());
                            return null;
                        }
                        if ("Connection reset".equals(sockex.getMessage())) {
                            log.trace(Markers.MSSQL, "Socket has been reset: port " + this.socket.getPort() + ", local port: " + this.socket.getLocalPort());
                            return null;
                        }
                        throw new ProtocolException("db.mssql.protocol.ProtocolException", sockex.getMessage());
                    }
                    catch (Exception ex) {
                        throw new ProtocolException("db.mssql.protocol.ProtocolException", ex.getMessage());
                    }
                    if (numRead == 0) {
                        log.warn(Markers.MSSQL, "Received 0 bytes???");
                    }
                    if (numRead == -1) {
                        return null;
                    }
                    this.readOffset += numRead;
                    this.needToReadMore = false;
                }
                if (this.readOffset < 4) {
                    this.needToReadMore = true;
                    continue;
                }
                if (expectSSL) {
                    if (this.readOffset > 4 && this.buffer[0] == 23 && this.buffer[1] == 3 && this.buffer[2] == 3) {
                        byte[] packetBytes = new byte[this.readOffset];
                        System.arraycopy(this.buffer, 0, packetBytes, 0, this.readOffset);
                        RawPacket pkt = new RawPacket(packetBytes);
                        this.readOffset = 0;
                        return pkt;
                    }
                } else if (this.buffer[0] == 23) {
                    short sslPktLen;
                    if (log.isTraceEnabled()) {
                        log.trace("Read TLS packet (probably) from " + String.valueOf(this.forwarder) + ", size so far: " + this.readOffset + ", read this time: " + readThisTime);
                    }
                    if (this.readOffset < (sslPktLen = DataTypeReader.readTwoByteInteger(this.buffer, 3)) + 5) {
                        if (log.isTraceEnabled()) {
                            log.trace("DEBUG - SSL packet is " + sslPktLen + " but have only received " + this.readOffset + " of it, must read more");
                        }
                        this.needToReadMore = true;
                        continue;
                    }
                    byte[] packetBytes = new byte[sslPktLen + 5];
                    System.arraycopy(this.buffer, 0, packetBytes, 0, sslPktLen + 5);
                    RawPacket pkt = new RawPacket(packetBytes);
                    if (this.readOffset - (sslPktLen + 5) > 0) {
                        System.arraycopy(this.buffer, sslPktLen + 5, this.buffer, 0, this.readOffset - (sslPktLen + 5));
                    }
                    this.readOffset -= sslPktLen + 5;
                    if (this.readOffset > 0 && this.buffer[0] != 23) {
                        throw new RuntimeException("Not a TLS packet");
                    }
                    if (this.readOffset >= 5) {
                        sslPktLen = DataTypeReader.readTwoByteInteger(this.buffer, 3);
                        if (this.readOffset < sslPktLen) {
                            this.needToReadMore = true;
                        }
                    } else {
                        this.needToReadMore = true;
                    }
                    return pkt;
                }
                if ((packetLen = this.buffer[0] == 83 ? DataTypeReader.readTwoByteIntegerLow(this.buffer, 4) : DataTypeReader.readTwoByteInteger(this.buffer, 2)) < 0) {
                    throw new ServerException("db.mssql.protocol.InternalError", "Negative packet length");
                }
                if (packetLen <= this.readOffset) break block27;
                this.needToReadMore = true;
                if (this.readOffset == this.buffer.length) break;
            }
            throw new ServerException("db.mssql.protocol.InternalError", "Unable to read packet -- too large");
        }
        byte[] packetBytes = new byte[packetLen];
        System.arraycopy(this.buffer, 0, packetBytes, 0, packetLen);
        RawPacket pkt = new RawPacket(packetBytes);
        if (this.readOffset > packetLen) {
            System.arraycopy(this.buffer, packetLen, this.buffer, 0, this.readOffset - packetLen);
            this.readOffset -= packetLen;
            this.needToReadMore = false;
        } else {
            this.readOffset = 0;
            this.needToReadMore = true;
        }
        return pkt;
    }

    public void close() {
        try {
            if (log.isTraceEnabled()) {
                log.trace(Markers.MSSQL, "NetworkPacketReader is closing: {} on port {}, local port {}", (Object)this.forwarder.toString(), (Object)this.socket.getPort(), (Object)this.socket.getLocalPort());
            }
            this.socket.close();
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    protected void logNetwork(byte[] bytes, int idx, int len) {
        if (!logNet.isDebugEnabled()) {
            return;
        }
        if (logNet.isTraceEnabled()) {
            String bytesStr = BinaryDump.getBinaryDump(bytes, idx, len);
            logNet.trace(Markers.MSSQL, "Connection " + this.forwarder.adapter.getConnection().getName() + " from " + this.forwarder.getReceiveFromName() + ":\n" + bytesStr);
        }
    }

    public SSLEngine getSslEngine() {
        return this.sslEngine;
    }

    public void setSslEngine(SSLEngine sslEngine) {
        this.sslEngine = sslEngine;
    }
}
