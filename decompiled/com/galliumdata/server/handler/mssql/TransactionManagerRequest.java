/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.ConnectionState;
import com.galliumdata.server.handler.mssql.MSSQLPacket;
import com.galliumdata.server.handler.mssql.RawPacketWriter;

public class TransactionManagerRequest
extends MSSQLPacket {
    private long transactionDescriptor;
    private int outstandingRequestCount;
    private short requestType;
    public static final int TM_GET_DTC_ADDRESS = 0;
    public static final int TM_PROPAGATE_XACT = 1;
    public static final int TM_BEGIN_XACT = 5;
    public static final int TM_PROMOTE_XACT = 6;
    public static final int TM_COMMIT_XACT = 7;
    public static final int TM_ROLLBACK_XACT = 8;
    public static final int TM_SAVE_XACT = 9;
    private byte[] propagateBuffer;
    private byte isolationLevel;
    private byte[] beginTransactionName;
    private byte[] commitTransactionName;
    private boolean commitTransactionBeginTransaction;
    private Byte commitTransactionIsolationLevel;
    private byte[] saveTransactionName;
    private byte[] rawBytes;

    public TransactionManagerRequest(ConnectionState connectionState) {
        super(connectionState);
        this.typeCode = (byte)14;
    }

    @Override
    public int readFromBytes(byte[] bytes, int offset, int numBytes) {
        int idx = offset;
        this.rawBytes = new byte[this.length - 8];
        System.arraycopy(bytes, idx += super.readFromBytes(bytes, offset, numBytes), this.rawBytes, 0, this.length - 8);
        return (idx += this.length - 8) - offset;
    }

    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        if (this.rawBytes != null) {
            return size + this.rawBytes.length;
        }
        size += 22;
        size += 2;
        switch (this.requestType) {
            case 0: {
                size += 2;
                break;
            }
            case 1: {
                size += 2;
                if (this.propagateBuffer == null || this.propagateBuffer.length <= 0) break;
                size += this.propagateBuffer.length;
                break;
            }
            case 5: {
                ++size;
                ++size;
                if (this.beginTransactionName == null) break;
                size += this.beginTransactionName.length;
                break;
            }
            case 7: 
            case 8: {
                ++size;
                if (this.commitTransactionName != null && this.commitTransactionName.length > 0) {
                    size += this.commitTransactionName.length;
                }
                ++size;
                if (this.commitTransactionIsolationLevel == null) break;
                ++size;
                ++size;
                if (this.beginTransactionName == null || this.beginTransactionName.length <= 0) break;
                size += this.beginTransactionName.length;
                break;
            }
            case 9: {
                ++size;
                if (this.saveTransactionName == null || this.saveTransactionName.length <= 0) break;
                size += this.saveTransactionName.length;
            }
        }
        return size;
    }

    @Override
    public void write(RawPacketWriter writer) {
        if (this.rawBytes != null) {
            writer.writeBytes(this.rawBytes, 0, this.rawBytes.length);
            return;
        }
        writer.writeFourByteIntegerLow(22);
        writer.writeFourByteIntegerLow(18);
        writer.writeTwoByteIntegerLow(2);
        writer.writeEightByteNumber(this.transactionDescriptor);
        writer.writeFourByteIntegerLow(this.outstandingRequestCount);
        writer.writeTwoByteIntegerLow(this.requestType);
        switch (this.requestType) {
            case 0: {
                writer.writeTwoByteIntegerLow(0);
                break;
            }
            case 1: {
                if (this.propagateBuffer == null || this.propagateBuffer.length == 0) {
                    writer.writeTwoByteIntegerLow(0);
                    break;
                }
                writer.writeTwoByteIntegerLow(this.propagateBuffer.length);
                writer.writeBytes(this.propagateBuffer, 0, this.propagateBuffer.length);
                break;
            }
            case 5: {
                writer.writeByte(this.isolationLevel);
                if (this.beginTransactionName == null || this.beginTransactionName.length == 0) {
                    writer.writeByte((byte)0);
                    break;
                }
                writer.writeByte((byte)this.beginTransactionName.length);
                writer.writeBytes(this.beginTransactionName, 0, this.beginTransactionName.length);
                break;
            }
            case 6: {
                break;
            }
            case 7: 
            case 8: {
                if (this.commitTransactionName == null || this.commitTransactionName.length == 0) {
                    writer.writeByte((byte)0);
                } else {
                    writer.writeByte((byte)this.commitTransactionName.length);
                    writer.writeBytes(this.commitTransactionName, 0, this.commitTransactionName.length);
                }
                byte flags = 0;
                if (this.commitTransactionBeginTransaction) {
                    flags = (byte)(flags | 1);
                }
                writer.writeByte(flags);
                if (this.commitTransactionIsolationLevel == null) break;
                writer.writeByte(this.commitTransactionIsolationLevel);
                if (this.beginTransactionName == null || this.beginTransactionName.length == 0) {
                    writer.writeByte((byte)0);
                    break;
                }
                writer.writeByte((byte)this.beginTransactionName.length);
                writer.writeBytes(this.beginTransactionName, 0, this.beginTransactionName.length);
                break;
            }
            case 9: {
                if (this.saveTransactionName == null || this.saveTransactionName.length == 0) {
                    writer.writeByte((byte)0);
                    break;
                }
                writer.writeByte((byte)this.saveTransactionName.length);
                writer.writeBytes(this.saveTransactionName, 0, this.saveTransactionName.length);
                break;
            }
            default: {
                throw new ServerException("db.mssql.protocol.UnknownTxReqType", this.requestType);
            }
        }
    }

    @Override
    public String getPacketType() {
        return "TransactionManagerRequest";
    }

    @Override
    public String toString() {
        Object s = "Tx request";
        if (this.rawBytes != null) {
            return (String)s + " - raw";
        }
        switch (this.requestType) {
            case 0: {
                s = (String)s + " - GET_DTC_ADDRESS";
                break;
            }
            case 1: {
                s = (String)s + " - PROPAGATE_XACT";
                break;
            }
            case 5: {
                s = (String)s + " - BEGIN_XACT";
                break;
            }
            case 6: {
                s = (String)s + " - PROMOTE_XACT";
                break;
            }
            case 7: {
                s = (String)s + " - COMMIT_XACT";
                break;
            }
            case 8: {
                s = (String)s + " - ROLLBACK_XACT";
                break;
            }
            case 9: {
                s = (String)s + " - SAVE_XACT";
                break;
            }
            default: {
                s = (String)s + " - unknown type " + this.requestType;
            }
        }
        return s;
    }
}
