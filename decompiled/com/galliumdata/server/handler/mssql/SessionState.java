/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.handler.mssql.DataTypeReader;

public class SessionState {
    private byte stateId;
    private byte[] stateValue;

    public int readFromBytes(byte[] bytes, int offset, int numBytes) {
        int idx = offset;
        this.stateId = bytes[idx];
        int len = bytes[++idx];
        ++idx;
        if (len == -1) {
            len = DataTypeReader.readFourByteIntegerLow(bytes, idx);
            idx += 2;
        }
        this.stateValue = new byte[len];
        System.arraycopy(bytes, idx, this.stateValue, 0, len);
        return idx - offset;
    }

    public byte getStateId() {
        return this.stateId;
    }

    public void setStateId(byte stateId) {
        this.stateId = stateId;
    }

    public byte[] getStateValue() {
        return this.stateValue;
    }

    public void setStateValue(byte[] stateValue) {
        this.stateValue = stateValue;
    }
}
