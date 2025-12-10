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
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

public class FederatedAuthenticationPacket
extends MSSQLPacket
implements ProxyObject {
    private byte[] payload;
    private byte[] nonce;
    private static final int NONCE_SIZE = 32;

    public FederatedAuthenticationPacket(ConnectionState connectionState) {
        super(connectionState);
        this.typeCode = (byte)8;
    }

    @Override
    public int readFromBytes(byte[] bytes, int offset, int numBytes) {
        int idx = offset;
        int totalLength = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        int payloadLength = DataTypeReader.readFourByteIntegerLow(bytes, idx += 4);
        idx += 4;
        if (payloadLength > 0) {
            this.payload = new byte[payloadLength];
            System.arraycopy(bytes, idx, this.payload, 0, payloadLength);
            idx += payloadLength;
        }
        if (totalLength >= payloadLength + 32) {
            this.nonce = new byte[32];
            System.arraycopy(bytes, idx, this.nonce, 0, 32);
            idx += 32;
        }
        return idx - offset;
    }

    @Override
    public int getSerializedSize() {
        int size = 4;
        size += 4;
        if (this.payload != null) {
            size += this.payload.length;
        }
        if (this.nonce != null) {
            size += 32;
        }
        return size;
    }

    @Override
    public void write(RawPacketWriter writer) {
        int size = 4;
        if (this.payload != null) {
            size += this.payload.length;
        }
        if (this.nonce != null) {
            size += 32;
        }
        writer.writeFourByteIntegerLow(size);
        if (this.payload != null) {
            writer.writeBytes(this.payload, 0, this.payload.length);
        }
        if (this.nonce != null) {
            writer.writeBytes(this.nonce, 0, this.nonce.length);
        }
    }

    @Override
    public String getPacketType() {
        return "FederatedAuthentication";
    }

    @Override
    public String toString() {
        return "Federated authentication: " + (String)(this.payload == null ? "[empty]" : "[" + this.payload.length + " bytes]");
    }

    public byte[] getPayload() {
        return this.payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public byte[] getNonce() {
        return this.nonce;
    }

    public void setNonce(byte[] nonce) {
        this.nonce = nonce;
    }

    @Override
    public Object getMember(String key) {
        switch (key) {
            case "payload": {
                return this.getPayload();
            }
            case "nonce": {
                return this.getNonce();
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
        return new String[]{"payload", "nonce", "packetType", "remove", "toString"};
    }

    @Override
    public boolean hasMember(String key) {
        switch (key) {
            case "payload": 
            case "nonce": 
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
            case "payload": {
                if (!value.hasArrayElements()) {
                    throw new ServerException("db.mssql.logic.ValueHasWrongType", "byte array", value);
                }
                int arraySize = (int)value.getArraySize();
                byte[] newPayload = new byte[arraySize];
                for (int i = 0; i < arraySize; ++i) {
                    Value elem = value.getArrayElement((long)i);
                    if (!elem.fitsInByte()) {
                        throw new ServerException("db.mssql.logic.ValueHasWrongType", "byte", elem);
                    }
                    newPayload[i] = elem.asByte();
                }
                this.setPayload(newPayload);
                break;
            }
            case "nonce": {
                if (!value.hasArrayElements()) {
                    throw new ServerException("db.mssql.logic.ValueHasWrongType", "byte array", value);
                }
                int arraySize = (int)value.getArraySize();
                byte[] newNonce = new byte[arraySize];
                for (int i = 0; i < arraySize; ++i) {
                    Value elem = value.getArrayElement((long)i);
                    if (!elem.fitsInByte()) {
                        throw new ServerException("db.mssql.logic.ValueHasWrongType", "byte", elem);
                    }
                    newNonce[i] = elem.asByte();
                }
                this.setNonce(newNonce);
                break;
            }
            default: {
                throw new ServerException("db.mssql.logic.NoSuchMember", key);
            }
        }
    }

    @Override
    public boolean removeMember(String key) {
        throw new ServerException("db.mssql.logic.CannotRemoveMember", key, "FederatedAuthentication packet");
    }
}
