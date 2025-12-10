/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.graalvm.polyglot.Value
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.ServerException;
import com.galliumdata.server.handler.mssql.ConnectionState;
import com.galliumdata.server.handler.mssql.DataTypeReader;
import com.galliumdata.server.handler.mssql.DataTypeWriter;
import com.galliumdata.server.handler.mssql.MSSQLPacket;
import com.galliumdata.server.handler.mssql.MSSQLUTF16String;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.log.Markers;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.graalvm.polyglot.Value;

public class PreLoginPacket
extends MSSQLPacket {
    private boolean option0;
    private byte majorVersion;
    private byte minorVersion;
    private short buildNumber;
    private short subBuildNumber;
    private boolean option1;
    private byte encryption;
    private boolean option2;
    private String instValidity;
    private boolean option3;
    private Integer threadId;
    private boolean option4;
    private boolean mars;
    private boolean option5;
    private UUID connectionGUID;
    private UUID activityGUID;
    private int activitySequence;
    private boolean option6;
    private byte fedAuthRequired;
    private boolean option7;
    private int nonce;
    private boolean isSSL;
    private byte[] sslData;

    public PreLoginPacket(ConnectionState connectionState) {
        super(connectionState);
    }

    public PreLoginPacket(ConnectionState connectionState, boolean ssl) {
        super(connectionState);
        this.isSSL = ssl;
    }

    @Override
    public int readFromBytes(byte[] bytes, int offset, int numBytes) {
        int idx = offset;
        idx += super.readFromBytes(bytes, offset, numBytes);
        if (this.isSSL) {
            this.sslData = new byte[numBytes - 8];
            System.arraycopy(bytes, idx, this.sslData, 0, numBytes - 8);
            return numBytes;
        }
        int valuesEnd = 0;
        block10: while (idx < offset + numBytes) {
            byte optionType = bytes[idx];
            ++idx;
            if (optionType == -1) break;
            short optionOffset = (short)(DataTypeReader.readTwoByteInteger(bytes, idx) + 8 + offset);
            short optionLength = DataTypeReader.readTwoByteInteger(bytes, idx += 2);
            idx += 2;
            valuesEnd = optionOffset + optionLength;
            switch (optionType) {
                case 0: {
                    if (optionLength != 6) {
                        throw new ServerException("db.mssql.protocol.ProtocolViolation", "PreLogin option 0 has invalid size: " + optionLength + ", was expecting 6");
                    }
                    this.option0 = true;
                    this.majorVersion = bytes[optionOffset];
                    this.minorVersion = bytes[optionOffset + 1];
                    this.buildNumber = DataTypeReader.readTwoByteInteger(bytes, optionOffset + 2);
                    this.subBuildNumber = DataTypeReader.readTwoByteInteger(bytes, optionOffset + 4);
                    continue block10;
                }
                case 1: {
                    if (optionLength != 1) {
                        throw new ServerException("db.mssql.protocol.ProtocolViolation", "PreLogin option 1 has invalid size: " + optionLength + ", was expecting 1");
                    }
                    this.option1 = true;
                    this.encryption = bytes[optionOffset];
                    continue block10;
                }
                case 2: {
                    this.option2 = true;
                    if (optionLength <= 1) continue block10;
                    MSSQLUTF16String s = new MSSQLUTF16String();
                    s.readFromBytes(bytes, optionOffset, optionLength);
                    this.instValidity = s.getString();
                    continue block10;
                }
                case 3: {
                    this.option3 = true;
                    if (optionLength <= 0) continue block10;
                    if (optionLength != 4) {
                        throw new ServerException("db.mssql.protocol.ProtocolViolation", "PreLogin option 3 has invalid size: " + optionLength + ", was expecting 4");
                    }
                    this.threadId = DataTypeReader.readFourByteInteger(bytes, optionOffset);
                    continue block10;
                }
                case 4: {
                    if (optionLength != 1) {
                        throw new ServerException("db.mssql.protocol.ProtocolViolation", "PreLogin option 4 has invalid size: " + optionLength + ", was expecting 1");
                    }
                    this.option4 = true;
                    this.mars = bytes[optionOffset] != 0;
                    continue block10;
                }
                case 5: {
                    if (optionLength != 0 && optionLength != 36) {
                        throw new ServerException("db.mssql.protocol.ProtocolViolation", "PreLogin option 5 has invalid size: " + optionLength + ", was expecting 36");
                    }
                    this.option5 = true;
                    if (optionLength <= 0) continue block10;
                    long l1 = DataTypeReader.readEightByteInteger(bytes, optionOffset);
                    long l2 = DataTypeReader.readEightByteInteger(bytes, optionOffset + 8);
                    this.connectionGUID = new UUID(l1, l2);
                    l1 = DataTypeReader.readEightByteInteger(bytes, optionOffset + 16);
                    l2 = DataTypeReader.readEightByteInteger(bytes, optionOffset + 24);
                    this.activityGUID = new UUID(l1, l2);
                    this.activitySequence = DataTypeReader.readFourByteInteger(bytes, optionOffset + 32);
                    continue block10;
                }
                case 6: {
                    if (optionLength != 1) {
                        throw new ServerException("db.mssql.protocol.ProtocolViolation", "PreLogin option 6 has invalid size: " + optionLength + ", was expecting 1");
                    }
                    this.option6 = true;
                    this.fedAuthRequired = bytes[optionOffset];
                    continue block10;
                }
                case 7: {
                    if (optionLength != 4) {
                        throw new ServerException("db.mssql.protocol.ProtocolViolation", "PreLogin option 7 has invalid size: " + optionLength + ", was expecting 4");
                    }
                    this.option7 = true;
                    this.nonce = DataTypeReader.readFourByteInteger(bytes, optionOffset);
                    continue block10;
                }
            }
            log.debug(Markers.MSSQL, "Unknown pre-login option type: " + optionType);
        }
        return valuesEnd - offset;
    }

    @Override
    public void read(RawPacketReader reader) {
        super.read(reader);
        if (this.isSSL) {
            this.sslData = reader.readBytes(this.length - 8);
            return;
        }
        byte[] bytes = reader.readBytes(reader.getNumUnreadBytes());
        block10: for (int idx = 0; idx < bytes.length; idx += 2) {
            byte optionType = bytes[idx];
            ++idx;
            if (optionType == -1) break;
            short optionOffset = DataTypeReader.readTwoByteInteger(bytes, idx);
            short optionLength = DataTypeReader.readTwoByteInteger(bytes, idx += 2);
            switch (optionType) {
                case 0: {
                    if (optionLength != 6) {
                        throw new ServerException("db.mssql.protocol.ProtocolViolation", "PreLogin option 0 has invalid size: " + optionLength + ", was expecting 6");
                    }
                    this.option0 = true;
                    this.majorVersion = bytes[optionOffset];
                    this.minorVersion = bytes[optionOffset + 1];
                    this.buildNumber = DataTypeReader.readTwoByteInteger(bytes, optionOffset + 2);
                    this.subBuildNumber = DataTypeReader.readTwoByteInteger(bytes, optionOffset + 4);
                    continue block10;
                }
                case 1: {
                    if (optionLength != 1) {
                        throw new ServerException("db.mssql.protocol.ProtocolViolation", "PreLogin option 1 has invalid size: " + optionLength + ", was expecting 1");
                    }
                    this.option1 = true;
                    this.encryption = bytes[optionOffset];
                    continue block10;
                }
                case 2: {
                    this.option2 = true;
                    MSSQLUTF16String s = new MSSQLUTF16String();
                    s.readFromBytes(bytes, optionOffset, optionLength);
                    this.instValidity = s.getString();
                    continue block10;
                }
                case 3: {
                    if (optionLength != 4) {
                        throw new ServerException("db.mssql.protocol.ProtocolViolation", "PreLogin option 3 has invalid size: " + optionLength + ", was expecting 4");
                    }
                    this.option3 = true;
                    this.threadId = DataTypeReader.readFourByteInteger(bytes, optionOffset);
                    continue block10;
                }
                case 4: {
                    if (optionLength != 1) {
                        throw new ServerException("db.mssql.protocol.ProtocolViolation", "PreLogin option 4 has invalid size: " + optionLength + ", was expecting 1");
                    }
                    this.option4 = true;
                    this.mars = bytes[optionOffset] != 0;
                    continue block10;
                }
                case 5: {
                    if (optionLength != 0 && optionLength != 36) {
                        throw new ServerException("db.mssql.protocol.ProtocolViolation", "PreLogin option 5 has invalid size: " + optionLength + ", was expecting 36");
                    }
                    this.option5 = true;
                    if (optionLength <= 0) continue block10;
                    long l1 = DataTypeReader.readEightByteInteger(bytes, optionOffset);
                    long l2 = DataTypeReader.readEightByteInteger(bytes, optionOffset + 8);
                    this.connectionGUID = new UUID(l1, l2);
                    l1 = DataTypeReader.readEightByteInteger(bytes, optionOffset + 16);
                    l2 = DataTypeReader.readEightByteInteger(bytes, optionOffset + 24);
                    this.activityGUID = new UUID(l1, l2);
                    this.activitySequence = DataTypeReader.readFourByteInteger(bytes, optionOffset + 32);
                    continue block10;
                }
                case 6: {
                    if (optionLength != 1) {
                        throw new ServerException("db.mssql.protocol.ProtocolViolation", "PreLogin option 6 has invalid size: " + optionLength + ", was expecting 1");
                    }
                    this.option6 = true;
                    this.fedAuthRequired = bytes[optionOffset];
                    continue block10;
                }
                case 7: {
                    if (optionLength != 4) {
                        throw new ServerException("db.mssql.protocol.ProtocolViolation", "PreLogin option 7 has invalid size: " + optionLength + ", was expecting 4");
                    }
                    this.option7 = true;
                    this.nonce = DataTypeReader.readFourByteInteger(bytes, optionOffset);
                    continue block10;
                }
            }
            log.debug(Markers.MSSQL, "Unknown pre-login option type: " + optionType);
        }
    }

    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        if (this.isSSL) {
            return size += this.sslData.length;
        }
        if (this.option0) {
            size += 5;
            size += 6;
        }
        if (this.option1) {
            size += 5;
            ++size;
        }
        if (this.option2) {
            size += 5;
            size = this.instValidity == null || this.instValidity.isEmpty() ? ++size : (size += this.instValidity.getBytes(StandardCharsets.UTF_16).length);
        }
        if (this.option3) {
            size += 5;
            if (this.threadId != 0) {
                size += 4;
            }
        }
        if (this.option4) {
            size += 5;
            ++size;
        }
        if (this.option5) {
            size += 5;
            if (this.connectionGUID != null) {
                size += 36;
            }
        }
        if (this.option6) {
            size += 5;
            ++size;
        }
        if (this.option7) {
            size += 5;
            size += 4;
        }
        return ++size;
    }

    @Override
    public void write(RawPacketWriter writer) {
        if (this.isSSL) {
            writer.writeBytes(this.sslData, 0, this.sslData.length);
            writer.getPacket().finalizeLength();
            return;
        }
        byte[] buffer = new byte[4000];
        int idx = 0;
        int descSize = 0;
        if (this.option0) {
            descSize += 5;
        }
        if (this.option1) {
            descSize += 5;
        }
        if (this.option2) {
            descSize += 5;
        }
        if (this.option3) {
            descSize += 5;
        }
        if (this.option4) {
            descSize += 5;
        }
        if (this.option5) {
            descSize += 5;
        }
        if (this.option6) {
            descSize += 5;
        }
        if (this.option7) {
            descSize += 5;
        }
        int valueIdx = idx + ++descSize;
        if (this.option0) {
            buffer[idx] = 0;
            DataTypeWriter.encodeTwoByteInteger(buffer, ++idx, (short)valueIdx);
            DataTypeWriter.encodeTwoByteInteger(buffer, idx += 2, (short)6);
            idx += 2;
            buffer[valueIdx] = this.majorVersion;
            buffer[++valueIdx] = this.minorVersion;
            DataTypeWriter.encodeTwoByteInteger(buffer, ++valueIdx, this.buildNumber);
            DataTypeWriter.encodeTwoByteInteger(buffer, valueIdx += 2, this.subBuildNumber);
            valueIdx += 2;
        }
        if (this.option1) {
            buffer[idx] = 1;
            DataTypeWriter.encodeTwoByteInteger(buffer, ++idx, (short)valueIdx);
            DataTypeWriter.encodeTwoByteInteger(buffer, idx += 2, (short)1);
            idx += 2;
            buffer[valueIdx] = this.encryption;
            ++valueIdx;
        }
        if (this.option2) {
            buffer[idx] = 2;
            DataTypeWriter.encodeTwoByteInteger(buffer, ++idx, (short)valueIdx);
            int len = this.instValidity == null || this.instValidity.isEmpty() ? 1 : this.instValidity.getBytes(StandardCharsets.UTF_16).length;
            DataTypeWriter.encodeTwoByteInteger(buffer, idx += 2, (short)len);
            idx += 2;
            if (this.instValidity == null || this.instValidity.isEmpty()) {
                buffer[valueIdx] = 0;
                ++valueIdx;
            } else {
                byte[] strBytes = this.instValidity.getBytes(StandardCharsets.UTF_16);
                System.arraycopy(strBytes, 0, buffer, valueIdx, strBytes.length);
                valueIdx += strBytes.length;
            }
        }
        if (this.option3) {
            buffer[idx] = 3;
            DataTypeWriter.encodeTwoByteInteger(buffer, ++idx, (short)valueIdx);
            idx += 2;
            if (this.threadId == null) {
                DataTypeWriter.encodeTwoByteInteger(buffer, idx, (short)0);
                idx += 2;
            } else {
                DataTypeWriter.encodeTwoByteInteger(buffer, idx, (short)4);
                idx += 2;
                DataTypeWriter.encodeFourByteInteger(buffer, valueIdx, this.threadId);
                valueIdx += 4;
            }
        }
        if (this.option4) {
            buffer[idx] = 4;
            DataTypeWriter.encodeTwoByteInteger(buffer, ++idx, (short)valueIdx);
            DataTypeWriter.encodeTwoByteInteger(buffer, idx += 2, (short)1);
            idx += 2;
            buffer[valueIdx] = this.mars ? (byte)1 : 0;
            ++valueIdx;
        }
        if (this.option5) {
            buffer[idx] = 5;
            DataTypeWriter.encodeTwoByteInteger(buffer, ++idx, (short)valueIdx);
            idx += 2;
            if (this.connectionGUID == null) {
                DataTypeWriter.encodeTwoByteInteger(buffer, idx, (short)0);
            } else {
                DataTypeWriter.encodeTwoByteInteger(buffer, idx, (short)36);
            }
            idx += 2;
            if (this.connectionGUID != null) {
                DataTypeWriter.encodeEightByteInteger(buffer, valueIdx, this.connectionGUID.getMostSignificantBits());
                DataTypeWriter.encodeEightByteInteger(buffer, valueIdx += 8, this.connectionGUID.getLeastSignificantBits());
                DataTypeWriter.encodeEightByteInteger(buffer, valueIdx += 8, this.activityGUID.getMostSignificantBits());
                DataTypeWriter.encodeEightByteInteger(buffer, valueIdx += 8, this.activityGUID.getLeastSignificantBits());
                DataTypeWriter.encodeFourByteInteger(buffer, valueIdx += 8, this.activitySequence);
                valueIdx += 4;
            }
        }
        if (this.option6) {
            buffer[idx] = 6;
            DataTypeWriter.encodeTwoByteInteger(buffer, ++idx, (short)valueIdx);
            DataTypeWriter.encodeTwoByteInteger(buffer, idx += 2, (short)1);
            idx += 2;
            buffer[valueIdx] = this.fedAuthRequired;
            ++valueIdx;
        }
        if (this.option7) {
            buffer[idx] = 7;
            DataTypeWriter.encodeTwoByteInteger(buffer, ++idx, (short)valueIdx);
            DataTypeWriter.encodeTwoByteInteger(buffer, idx += 2, (short)4);
            idx += 2;
            DataTypeWriter.encodeFourByteInteger(buffer, valueIdx, this.nonce);
            valueIdx += 4;
        }
        buffer[idx] = -1;
        ++idx;
        writer.writeBytes(buffer, 0, valueIdx);
    }

    @Override
    public String getPacketType() {
        return "PreLogin";
    }

    @Override
    public String toString() {
        return "Pre-login v." + this.majorVersion + "." + this.minorVersion + " bld " + this.buildNumber;
    }

    public boolean isOption0() {
        return this.option0;
    }

    public void setOption0(boolean option0) {
        this.option0 = option0;
    }

    public byte getMajorVersion() {
        return this.majorVersion;
    }

    public void setMajorVersion(byte majorVersion) {
        this.majorVersion = majorVersion;
        this.option0 = true;
        this.setModified();
    }

    public byte getMinorVersion() {
        return this.minorVersion;
    }

    public void setMinorVersion(byte minorVersion) {
        this.minorVersion = minorVersion;
        this.option0 = true;
        this.setModified();
    }

    public short getBuildNumber() {
        return this.buildNumber;
    }

    public void setBuildNumber(short buildNumber) {
        this.buildNumber = buildNumber;
        this.option0 = true;
        this.setModified();
    }

    public short getSubBuildNumber() {
        return this.subBuildNumber;
    }

    public void setSubBuildNumber(short subBuildNumber) {
        this.subBuildNumber = subBuildNumber;
        this.option0 = true;
        this.setModified();
    }

    public boolean isOption1() {
        return this.option1;
    }

    public void setOption1(boolean option1) {
        this.option1 = option1;
    }

    public byte getEncryption() {
        return this.encryption;
    }

    public void setEncryption(byte encryption) {
        this.encryption = encryption;
        this.option1 = true;
        this.setModified();
    }

    public boolean isOption2() {
        return this.option2;
    }

    public void setOption2(boolean option2) {
        this.option2 = option2;
    }

    public String getInstValidity() {
        return this.instValidity;
    }

    public void setInstValidity(String instValidity) {
        this.instValidity = instValidity;
        this.option2 = true;
        this.setModified();
    }

    public boolean isOption3() {
        return this.option3;
    }

    public void setOption3(boolean option3) {
        this.option3 = option3;
    }

    public int getThreadId() {
        return this.threadId;
    }

    public void setThreadId(int threadId) {
        this.threadId = threadId;
        this.option3 = true;
        this.setModified();
    }

    public boolean isOption4() {
        return this.option4;
    }

    public void setOption4(boolean option4) {
        this.option4 = option4;
    }

    public boolean getMars() {
        return this.mars;
    }

    public void setMars(boolean mars) {
        this.mars = mars;
        this.option4 = true;
        this.setModified();
    }

    public boolean isOption5() {
        return this.option5;
    }

    public void setOption5(boolean option5) {
        this.option5 = option5;
    }

    public UUID getConnectionGUID() {
        return this.connectionGUID;
    }

    public void setConnectionGUID(UUID connectionGUID) {
        this.connectionGUID = connectionGUID;
        this.option5 = true;
        this.setModified();
    }

    public void setConnectionGUID(String str) {
        this.setConnectionGUID(UUID.fromString(str));
    }

    public UUID getActivityGUID() {
        return this.activityGUID;
    }

    public void setActivityGUID(UUID activityGUID) {
        this.activityGUID = activityGUID;
        this.option5 = true;
        this.setModified();
    }

    public void setActivityGUID(String str) {
        this.setActivityGUID(UUID.fromString(str));
    }

    public int getActivitySequence() {
        return this.activitySequence;
    }

    public void setActivitySequence(int activitySequence) {
        this.activitySequence = activitySequence;
        this.option5 = true;
        this.setModified();
    }

    public boolean isOption6() {
        return this.option6;
    }

    public void setOption6(boolean option6) {
        this.option6 = option6;
    }

    public byte getFedAuthRequired() {
        return this.fedAuthRequired;
    }

    public void setFedAuthRequired(byte fedAuthRequired) {
        this.fedAuthRequired = fedAuthRequired;
        this.option6 = true;
        this.setModified();
    }

    public boolean isOption7() {
        return this.option7;
    }

    public void setOption7(boolean option7) {
        this.option7 = option7;
    }

    public int getNonce() {
        return this.nonce;
    }

    public void setNonce(int nonce) {
        this.nonce = nonce;
        this.option7 = true;
        this.setModified();
    }

    public boolean isSSL() {
        return this.isSSL;
    }

    public void setSSL(boolean SSL) {
        this.isSSL = SSL;
    }

    public byte[] getSslData() {
        return this.sslData;
    }

    public void setSslData(byte[] buf) {
        this.sslData = buf;
    }

    @Override
    public Object getMember(String key) {
        switch (key) {
            case "option0": {
                return this.option0;
            }
            case "majorVersion": {
                return this.majorVersion;
            }
            case "minorVersion": {
                return this.minorVersion;
            }
            case "buildNumber": {
                return this.buildNumber;
            }
            case "subBuildNumber": {
                return this.subBuildNumber;
            }
            case "option1": {
                return this.option1;
            }
            case "encryption": {
                return this.encryption;
            }
            case "option2": {
                return this.option2;
            }
            case "instValidity": {
                return this.instValidity;
            }
            case "option3": {
                return this.option3;
            }
            case "threadId": {
                return this.threadId;
            }
            case "option4": {
                return this.option4;
            }
            case "mars": {
                return this.mars;
            }
            case "option5": {
                return this.option5;
            }
            case "connectionGUID": {
                return this.connectionGUID;
            }
            case "activityGUID": {
                return this.activityGUID;
            }
            case "activitySequence": {
                return this.activitySequence;
            }
            case "option6": {
                return this.option6;
            }
            case "fedAuthRequired": {
                return this.fedAuthRequired;
            }
            case "option7": {
                return this.option7;
            }
            case "nonce": {
                return this.nonce;
            }
            case "isSSL": {
                return this.isSSL;
            }
        }
        return super.getMember(key);
    }

    @Override
    public Object getMemberKeys() {
        return new String[]{"option0", "majorVersion", "minorVersion", "buildNumber", "subBuildNumber", "option1", "encryption", "option2", "instValidity", "option3", "threadId", "option4", "mars", "option5", "connectionGUID", "activityGUID", "activitySequence", "option6", "fedAuthRequired", "option7", "nonce", "isSSL"};
    }

    @Override
    public boolean hasMember(String key) {
        switch (key) {
            case "option0": 
            case "majorVersion": 
            case "minorVersion": 
            case "buildNumber": 
            case "subBuildNumber": 
            case "option1": 
            case "encryption": 
            case "option2": 
            case "instValidity": 
            case "option3": 
            case "threadId": 
            case "option4": 
            case "mars": 
            case "option5": 
            case "connectionGUID": 
            case "activityGUID": 
            case "activitySequence": 
            case "option6": 
            case "fedAuthRequired": 
            case "option7": 
            case "nonce": 
            case "isSSL": {
                return true;
            }
        }
        return false;
    }

    @Override
    public void putMember(String key, Value value) {
        switch (key) {
            case "option0": {
                this.setOption0(value.asBoolean());
                break;
            }
            case "majorVersion": {
                this.setMajorVersion(value.asByte());
                break;
            }
            case "minorVersion": {
                this.setMinorVersion(value.asByte());
                break;
            }
            case "buildNumber": {
                this.setBuildNumber(value.asShort());
                break;
            }
            case "subBuildNumber": {
                this.setSubBuildNumber(value.asShort());
                break;
            }
            case "option1": {
                this.setOption1(value.asBoolean());
                break;
            }
            case "encryption": {
                this.setEncryption(value.asByte());
                break;
            }
            case "option2": {
                this.setOption2(value.asBoolean());
                break;
            }
            case "instValidity": {
                this.setInstValidity(value.asString());
                break;
            }
            case "option3": {
                this.setOption3(value.asBoolean());
                break;
            }
            case "HREADId": {
                this.setThreadId(value.asInt());
                break;
            }
            case "option4": {
                this.setOption4(value.asBoolean());
                break;
            }
            case "mars": {
                this.setMars(value.asBoolean());
                break;
            }
            case "option5": {
                this.setOption5(value.asBoolean());
                break;
            }
            case "connectionGUID": {
                this.setConnectionGUID(value.asString());
                break;
            }
            case "activityGUID": {
                this.setActivityGUID(value.asString());
                break;
            }
            case "activitySequence": {
                this.setActivitySequence(value.asInt());
                break;
            }
            case "option6": {
                this.setOption6(value.asBoolean());
                break;
            }
            case "fedAuthRequired": {
                this.setFedAuthRequired(value.asByte());
                break;
            }
            case "option7": {
                this.setOption7(value.asBoolean());
                break;
            }
            case "nonce": {
                this.setNonce(value.asInt());
                break;
            }
            case "isSSL": {
                this.setSSL(value.asBoolean());
                break;
            }
            default: {
                throw new ServerException("db.mssql.logic.NoSuchMember", key);
            }
        }
    }

    @Override
    public boolean removeMember(String key) {
        throw new ServerException("db.mssql.logic.CannotRemoveMember", key, "PreLogin packet");
    }
}
