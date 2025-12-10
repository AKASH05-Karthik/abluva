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
import com.galliumdata.server.handler.mssql.DataTypeWriter;
import com.galliumdata.server.handler.mssql.MSSQLPacket;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.loginfeatures.Login7Feature;
import com.galliumdata.server.handler.mssql.loginfeatures.Login7FeatureAzureSQLDNSCaching;
import com.galliumdata.server.handler.mssql.loginfeatures.Login7FeatureColumnEncryption;
import com.galliumdata.server.handler.mssql.loginfeatures.Login7FeatureDataClassification;
import com.galliumdata.server.handler.mssql.loginfeatures.Login7FeatureGlobalTransactions;
import com.galliumdata.server.handler.mssql.loginfeatures.Login7FeatureSessionRecovery;
import com.galliumdata.server.handler.mssql.loginfeatures.Login7FeatureUTF8Support;
import com.galliumdata.server.handler.mssql.loginfeatures.Login7FeatureUnknown;
import com.galliumdata.server.log.Markers;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

public class Login7Packet
extends MSSQLPacket
implements ProxyObject {
    private int tdsVersion;
    private int tdsRevision;
    private int packetSize;
    private byte[] clientProgramVersion = new byte[4];
    private long clientPid;
    private long connectionPid;
    private boolean fByteOrder;
    private boolean fChar;
    private byte fFloat;
    private boolean fDumpLoad;
    private boolean fUseDB;
    private boolean fDatabase;
    private boolean fSetLang;
    private boolean fLanguage;
    private boolean fODBC;
    private boolean fTranBoundary;
    private boolean fCacheConnect;
    private byte fUserType;
    private boolean fIntSecurity;
    private byte fSQLType;
    private boolean fOLEDB;
    private boolean fReadOnlyIntent;
    private boolean fChangePassword;
    private boolean fUserInstance;
    private boolean fSendYukonBinaryXML;
    private boolean fUnknownCollationHandling;
    private boolean fExtension;
    private int clientTimeZone;
    private int clientLcid;
    private String hostname;
    private String username;
    private String password;
    private String appName;
    private String serverName;
    private int extensionOffset;
    private boolean extensionsInline;
    private String clientName;
    private String language;
    private String database;
    private byte[] clientId;
    private byte[] sspi;
    private String attachDbFile;
    private String changePassword;
    private List<Login7Feature> features = new ArrayList<Login7Feature>();

    public Login7Packet(ConnectionState connectionState) {
        super(connectionState);
    }

    @Override
    public int readFromBytes(byte[] bytes, int offset, int numBytes) {
        int idx = offset;
        int loginLength = DataTypeReader.readFourByteIntegerLow(bytes, idx += super.readFromBytes(bytes, offset, numBytes));
        this.tdsVersion = DataTypeReader.readFourByteIntegerLow(bytes, idx += 4);
        idx += 4;
        switch (this.tdsVersion) {
            case 0x7000000: 
            case 0x7010000: 
            case 0x70000000: {
                this.connectionState.setTdsMajorVersion(7);
                this.connectionState.setTdsMinorVersion(0);
                break;
            }
            case 0x71000000: {
                this.connectionState.setTdsMajorVersion(7);
                this.connectionState.setTdsMinorVersion(1);
                break;
            }
            case 0x71000001: {
                this.connectionState.setTdsMajorVersion(7);
                this.connectionState.setTdsMinorVersion(1);
                this.connectionState.setTdsRevision(1);
                break;
            }
            case 1913192450: {
                this.connectionState.setTdsMajorVersion(7);
                this.connectionState.setTdsMinorVersion(2);
                break;
            }
            case 1930035203: 
            case 1930100739: {
                this.connectionState.setTdsMajorVersion(7);
                this.connectionState.setTdsMinorVersion(3);
                break;
            }
            case 0x74000004: {
                this.connectionState.setTdsMajorVersion(7);
                this.connectionState.setTdsMinorVersion(4);
                break;
            }
            default: {
                throw new ServerException("db.mssql.protocol.TDSVersionNotSupported", this.tdsVersion);
            }
        }
        this.packetSize = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        System.arraycopy(bytes, idx += 4, this.clientProgramVersion, 0, 4);
        this.clientPid = DataTypeReader.readFourByteIntegerLow(bytes, idx += 4);
        this.connectionPid = DataTypeReader.readFourByteIntegerLow(bytes, idx += 4);
        byte options1 = bytes[idx += 4];
        ++idx;
        this.fByteOrder = (options1 & 1) != 0;
        this.fChar = (options1 & 2) != 0;
        this.fFloat = (byte)((options1 & 0xC) >> 2);
        this.fDumpLoad = (options1 & 0x10) != 0;
        this.fUseDB = (options1 & 0x20) != 0;
        this.fDatabase = (options1 & 0x40) != 0;
        this.fSetLang = (options1 & 0xFFFFFF80) != 0;
        byte options2 = bytes[idx];
        ++idx;
        this.fLanguage = (options2 & 1) != 0;
        this.fODBC = (options2 & 2) != 0;
        this.fTranBoundary = (options2 & 4) != 0;
        this.fCacheConnect = (options2 & 8) != 0;
        this.fUserType = (byte)((options2 & 0x70) >> 4);
        this.fIntSecurity = (options2 & 0x80) != 0;
        byte typeFlags = bytes[idx];
        this.fSQLType = (byte)(typeFlags & 0xF);
        this.fOLEDB = (typeFlags & 0x10) != 0;
        this.fReadOnlyIntent = (typeFlags & 0x20) != 0;
        byte options3 = bytes[++idx];
        ++idx;
        this.fChangePassword = (options3 & 1) != 0;
        boolean bl = this.fSendYukonBinaryXML = (options3 & 2) != 0;
        if (this.fSendYukonBinaryXML) {
            this.connectionState.setBinaryXml(true);
            log.trace(Markers.MSSQL, "Login packet specifies that XML will be binary-encoded");
        }
        this.fUserInstance = (options3 & 4) != 0;
        this.fUnknownCollationHandling = (options3 & 8) != 0;
        this.fExtension = (options3 & 0x10) != 0;
        this.clientTimeZone = DataTypeReader.readFourByteIntegerLow(bytes, idx);
        this.clientLcid = DataTypeReader.readFourByteIntegerLow(bytes, idx += 4);
        this.hostname = this.readString(bytes, offset, idx += 4);
        this.username = this.readString(bytes, offset, idx += 4);
        short off = DataTypeReader.readTwoByteIntegerLow(bytes, idx += 4);
        short len = DataTypeReader.readTwoByteIntegerLow(bytes, idx += 2);
        idx += 2;
        if (len > 0) {
            byte[] passwordBytes = new byte[len * 2];
            System.arraycopy(bytes, offset + 8 + off, passwordBytes, 0, len * 2);
            for (int i = 0; i < passwordBytes.length; ++i) {
                int n = i;
                passwordBytes[n] = (byte)(passwordBytes[n] ^ 0xFFFFFFA5);
                byte lowBits = (byte)((passwordBytes[i] & 0xF) << 4);
                byte highBits = (byte)((passwordBytes[i] & 0xF0) >> 4);
                passwordBytes[i] = (byte)(highBits | lowBits);
            }
            this.password = new String(passwordBytes, StandardCharsets.UTF_16LE);
        }
        this.appName = this.readString(bytes, offset, idx);
        this.serverName = this.readString(bytes, offset, idx += 4);
        idx += 4;
        if (this.isfExtension()) {
            off = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
            len = DataTypeReader.readTwoByteIntegerLow(bytes, idx += 2);
            idx += 2;
            this.extensionOffset = DataTypeReader.readFourByteIntegerLow(bytes, offset + 8 + off);
        } else {
            idx += 4;
        }
        short clientNameIdx = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        if (this.extensionOffset <= clientNameIdx) {
            this.extensionsInline = true;
        }
        this.clientName = this.readString(bytes, offset, idx);
        this.language = this.readString(bytes, offset, idx += 4);
        this.database = this.readString(bytes, offset, idx += 4);
        this.clientId = new byte[6];
        System.arraycopy(bytes, idx += 4, this.clientId, 0, 6);
        short sspiOffset = DataTypeReader.readTwoByteIntegerLow(bytes, idx += 6);
        short sspiLen = DataTypeReader.readTwoByteIntegerLow(bytes, idx += 2);
        short attachDbFileIdx = DataTypeReader.readTwoByteIntegerLow(bytes, idx += 2);
        this.attachDbFile = this.readString(bytes, offset, idx);
        idx += 4;
        if (this.fChangePassword) {
            short changePasswordIdx = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
            this.changePassword = this.readString(bytes, offset, changePasswordIdx);
        }
        int sspiLenLong = DataTypeReader.readFourByteIntegerLow(bytes, idx += 4);
        idx += 4;
        if (sspiLen != 0) {
            if (sspiLen < Short.MAX_VALUE) {
                this.sspi = new byte[sspiLen];
                System.arraycopy(bytes, offset + 8 + sspiOffset, this.sspi, 0, sspiLen);
            } else {
                if (sspiLenLong == 0) {
                    sspiLenLong = sspiLen;
                }
                this.sspi = new byte[sspiLenLong];
                System.arraycopy(bytes, offset + 8 + sspiOffset, this.sspi, 0, sspiLenLong);
            }
        }
        if (this.extensionOffset > 0) {
            idx = offset + 8 + this.extensionOffset;
            while (idx - offset - 8 < loginLength) {
                if (bytes[idx] == -1) {
                    ++idx;
                    break;
                }
                idx += this.readFeature(bytes, idx);
            }
        }
        if (log.isTraceEnabled()) {
            log.trace(Markers.MSSQL, "Login as " + this.username + " - column-level encryption: " + this.connectionState.isColumnEncryptionInUse());
        }
        return idx - offset;
    }

    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        size += 4;
        size += 4;
        size += 4;
        size += 4;
        size += 4;
        size += 4;
        size += 4;
        size += 4;
        size += 4;
        size += 4;
        size += this.hostname.length() * 2;
        size += 4;
        size += this.username.length() * 2;
        size += 4;
        if (this.password != null) {
            size += this.password.length() * 2;
        }
        size += 4;
        size += this.appName.length() * 2;
        size += 4;
        size += this.serverName.length() * 2;
        size += 4;
        size += 4;
        size += this.clientName.length() * 2;
        size += 4;
        size += this.language.length() * 2;
        size += 4;
        size += this.database.length() * 2;
        size += 6;
        size += 4;
        size += 4;
        size += this.attachDbFile.length() * 2;
        size += 4;
        if (this.changePassword != null) {
            size += this.changePassword.length() * 2;
        }
        size += 4;
        if (this.sspi != null) {
            size += this.sspi.length;
        }
        if (this.isfExtension()) {
            size += 4;
        }
        for (Login7Feature feature : this.getFeatures()) {
            size += feature.getSerializedSize();
        }
        if (this.getFeatures().size() > 0 || this.isfExtension()) {
            ++size;
        }
        return size;
    }

    @Override
    public void write(RawPacketWriter writer) {
        int idx;
        writer.writeFourByteIntegerLow(this.getSerializedSize() - 8);
        writer.writeFourByteIntegerLow(this.tdsVersion);
        writer.writeFourByteIntegerLow(this.packetSize);
        writer.writeBytes(this.clientProgramVersion, 0, 4);
        writer.writeFourByteIntegerLow((int)this.clientPid);
        writer.writeFourByteIntegerLow((int)this.connectionPid);
        byte options1 = 0;
        if (this.fByteOrder) {
            options1 = (byte)(options1 | 1);
        }
        if (this.fChar) {
            options1 = (byte)(options1 | 2);
        }
        options1 = (byte)(options1 | (byte)((this.fFloat & 3) << 2));
        if (this.fDumpLoad) {
            options1 = (byte)(options1 | 0x10);
        }
        if (this.fUseDB) {
            options1 = (byte)(options1 | 0x20);
        }
        if (this.fDatabase) {
            options1 = (byte)(options1 | 0x40);
        }
        if (this.fSetLang) {
            options1 = (byte)(options1 | 0xFFFFFF80);
        }
        writer.writeByte(options1);
        byte options2 = 0;
        if (this.fLanguage) {
            options2 = (byte)(options2 | 1);
        }
        if (this.fODBC) {
            options2 = (byte)(options2 | 2);
        }
        if (this.fTranBoundary) {
            options2 = (byte)(options2 | 4);
        }
        if (this.fCacheConnect) {
            options2 = (byte)(options2 | 8);
        }
        options2 = (byte)(options2 | (byte)((this.fUserType & 7) << 4));
        if (this.fIntSecurity) {
            options2 = (byte)(options2 | 0x80);
        }
        writer.writeByte(options2);
        byte typeFlags = this.fSQLType;
        if (this.fOLEDB) {
            typeFlags = (byte)(typeFlags | 0x10);
        }
        if (this.fReadOnlyIntent) {
            typeFlags = (byte)(typeFlags | 0x20);
        }
        writer.writeByte(typeFlags);
        byte options3 = 0;
        if (this.fChangePassword) {
            options3 = (byte)(options3 | 1);
        }
        if (this.fSendYukonBinaryXML) {
            options3 = (byte)(options3 | 2);
        }
        if (this.fUserInstance) {
            options3 = (byte)(options3 | 4);
        }
        if (this.fUnknownCollationHandling) {
            options3 = (byte)(options3 | 8);
        }
        if (this.fExtension) {
            options3 = (byte)(options3 | 0x10);
        }
        writer.writeByte(options3);
        writer.writeFourByteIntegerLow(this.clientTimeZone);
        writer.writeFourByteIntegerLow(this.clientLcid);
        int bufferOffset = writer.getPacket().getWriteIndex() - 8;
        byte[] buffer = new byte[8000];
        int hostnameIdx = idx = 0;
        DataTypeWriter.encodeTwoByteIntegerLow(buffer, idx += 2, (short)this.hostname.length());
        int usernameIdx = idx += 2;
        DataTypeWriter.encodeTwoByteIntegerLow(buffer, idx += 2, (short)this.username.length());
        int passwordIdx = idx += 2;
        idx += 2;
        if (this.password != null) {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, idx, (short)this.password.length());
            idx += 2;
        } else {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, idx, (short)0);
            idx += 2;
        }
        int appNameIdx = idx;
        DataTypeWriter.encodeTwoByteIntegerLow(buffer, idx += 2, (short)this.appName.length());
        int serverNameIdx = idx += 2;
        DataTypeWriter.encodeTwoByteIntegerLow(buffer, idx += 2, (short)this.serverName.length());
        idx += 2;
        int extensionOffsetIdx = 0;
        if (this.isfExtension()) {
            extensionOffsetIdx = idx;
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, idx += 2, (short)4);
            idx += 2;
        } else {
            buffer[idx] = 0;
            buffer[idx + 1] = 0;
            buffer[idx + 2] = 0;
            buffer[idx + 3] = 0;
            idx += 4;
        }
        int clientNameIdx = idx;
        DataTypeWriter.encodeTwoByteIntegerLow(buffer, idx += 2, (short)this.clientName.length());
        int languageIdx = idx += 2;
        DataTypeWriter.encodeTwoByteIntegerLow(buffer, idx += 2, (short)this.language.length());
        int databaseIdx = idx += 2;
        DataTypeWriter.encodeTwoByteIntegerLow(buffer, idx += 2, (short)this.database.length());
        System.arraycopy(this.clientId, 0, buffer, idx += 2, 6);
        int sspiIdx = idx += 6;
        if (this.sspi == null) {
            buffer[idx] = 0;
            buffer[idx + 1] = 0;
            buffer[idx + 2] = 0;
            buffer[idx + 3] = 0;
            idx += 4;
        } else if (this.sspi.length < Short.MAX_VALUE) {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, idx += 2, (short)this.sspi.length);
            idx += 2;
        } else {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, idx += 2, (short)Short.MAX_VALUE);
            idx += 2;
        }
        int attachDbFileIdx = idx;
        DataTypeWriter.encodeTwoByteIntegerLow(buffer, idx += 2, (short)this.attachDbFile.length());
        int changePasswordIdx = idx += 2;
        idx += 2;
        if (this.changePassword != null) {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, idx, (short)this.changePassword.length());
        }
        idx += 2;
        if (this.sspi == null || this.sspi.length < Short.MAX_VALUE) {
            DataTypeWriter.encodeFourByteIntegerLow(buffer, idx, 0);
            idx += 4;
        } else {
            DataTypeWriter.encodeFourByteIntegerLow(buffer, idx, this.sspi.length);
            idx += 4;
        }
        DataTypeWriter.encodeTwoByteIntegerLow(buffer, hostnameIdx, (short)(idx + bufferOffset));
        byte[] strBytes = this.hostname.getBytes(StandardCharsets.UTF_16LE);
        System.arraycopy(strBytes, 0, buffer, idx, strBytes.length);
        idx += strBytes.length;
        if (!this.username.isEmpty()) {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, usernameIdx, (short)(idx + bufferOffset));
            usernameIdx = 0;
            strBytes = this.username.getBytes(StandardCharsets.UTF_16LE);
            System.arraycopy(strBytes, 0, buffer, idx, strBytes.length);
            idx += strBytes.length;
        }
        if (this.password != null && !this.password.isEmpty()) {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, passwordIdx, (short)(idx + bufferOffset));
            passwordIdx = 0;
            strBytes = this.password.getBytes(StandardCharsets.UTF_16LE);
            int i = 0;
            while (i < strBytes.length) {
                byte lowBits = (byte)((strBytes[i] & 0xF) << 4);
                byte highBits = (byte)((strBytes[i] & 0xF0) >> 4);
                strBytes[i] = (byte)(highBits | lowBits);
                int n = i++;
                strBytes[n] = (byte)(strBytes[n] ^ 0xFFFFFFA5);
            }
            System.arraycopy(strBytes, 0, buffer, idx, strBytes.length);
            idx += strBytes.length;
        }
        if (!this.appName.isEmpty()) {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, appNameIdx, (short)(idx + bufferOffset));
            appNameIdx = 0;
            strBytes = this.appName.getBytes(StandardCharsets.UTF_16LE);
            System.arraycopy(strBytes, 0, buffer, idx, strBytes.length);
            idx += strBytes.length;
        }
        if (!this.serverName.isEmpty()) {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, serverNameIdx, (short)(idx + bufferOffset));
            serverNameIdx = 0;
            strBytes = this.serverName.getBytes(StandardCharsets.UTF_16LE);
            System.arraycopy(strBytes, 0, buffer, idx, strBytes.length);
            idx += strBytes.length;
        }
        int extensionsIdx = 0;
        if (extensionOffsetIdx > 0) {
            int extensionStart = idx + bufferOffset;
            if (this.extensionsInline) {
                DataTypeWriter.encodeTwoByteIntegerLow(buffer, extensionOffsetIdx, (short)(idx + bufferOffset));
                DataTypeWriter.encodeFourByteIntegerLow(buffer, idx, extensionStart + 4);
                idx += 4;
                extensionsIdx = 0;
                if (this.features.size() > 0) {
                    RawPacketWriter extensionsWriter = new RawPacketWriter(this.connectionState, this, null);
                    for (Login7Feature feature : this.features) {
                        feature.write(extensionsWriter);
                    }
                    byte[] extensionsBytes = extensionsWriter.getPacket().getWrittenBuffer();
                    System.arraycopy(extensionsBytes, 8, buffer, idx, extensionsBytes.length - 8);
                    idx += extensionsBytes.length - 8;
                }
                buffer[idx] = -1;
                ++idx;
            } else {
                DataTypeWriter.encodeTwoByteIntegerLow(buffer, extensionOffsetIdx, (short)extensionStart);
                extensionsIdx = idx;
                idx += 4;
            }
        }
        DataTypeWriter.encodeTwoByteIntegerLow(buffer, clientNameIdx, (short)(idx + bufferOffset));
        if (!this.clientName.isEmpty()) {
            clientNameIdx = 0;
            strBytes = this.clientName.getBytes(StandardCharsets.UTF_16LE);
            System.arraycopy(strBytes, 0, buffer, idx, strBytes.length);
            idx += strBytes.length;
        }
        DataTypeWriter.encodeTwoByteIntegerLow(buffer, languageIdx, (short)(idx + bufferOffset));
        if (!this.language.isEmpty()) {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, languageIdx, (short)(idx + bufferOffset));
            languageIdx = 0;
            strBytes = this.language.getBytes(StandardCharsets.UTF_16LE);
            System.arraycopy(strBytes, 0, buffer, idx, strBytes.length);
            idx += strBytes.length;
        }
        if (!this.database.isEmpty()) {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, databaseIdx, (short)(idx + bufferOffset));
            databaseIdx = 0;
            strBytes = this.database.getBytes(StandardCharsets.UTF_16LE);
            System.arraycopy(strBytes, 0, buffer, idx, strBytes.length);
            idx += strBytes.length;
        } else {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, databaseIdx, (short)(idx + bufferOffset));
        }
        DataTypeWriter.encodeTwoByteIntegerLow(buffer, attachDbFileIdx, (short)(idx + bufferOffset));
        attachDbFileIdx = 0;
        if (!this.attachDbFile.isEmpty()) {
            strBytes = this.attachDbFile.getBytes(StandardCharsets.UTF_16LE);
            System.arraycopy(strBytes, 0, buffer, idx, strBytes.length);
            idx += strBytes.length;
        }
        DataTypeWriter.encodeTwoByteIntegerLow(buffer, changePasswordIdx, (short)(idx + bufferOffset));
        changePasswordIdx = 0;
        if (this.changePassword != null && !this.changePassword.isEmpty()) {
            strBytes = this.changePassword.getBytes(StandardCharsets.UTF_16LE);
            System.arraycopy(strBytes, 0, buffer, idx, strBytes.length);
            idx += strBytes.length;
        }
        DataTypeWriter.encodeTwoByteIntegerLow(buffer, sspiIdx, (short)(idx + bufferOffset));
        if (this.sspi != null && this.sspi.length != 0) {
            if (this.sspi.length < Short.MAX_VALUE) {
                System.arraycopy(this.sspi, 0, buffer, idx, this.sspi.length);
                idx += this.sspi.length;
            } else {
                DataTypeWriter.encodeTwoByteIntegerLow(buffer, sspiIdx, (short)0);
                System.arraycopy(this.sspi, 0, buffer, idx, this.sspi.length);
                idx += this.sspi.length;
            }
        }
        short dataEnd = (short)(idx + bufferOffset);
        if (usernameIdx > 0) {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, usernameIdx, dataEnd);
        }
        if (passwordIdx > 0) {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, passwordIdx, dataEnd);
        }
        if (appNameIdx > 0) {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, appNameIdx, dataEnd);
        }
        if (serverNameIdx > 0) {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, serverNameIdx, dataEnd);
        }
        if (clientNameIdx > 0) {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, clientNameIdx, dataEnd);
        }
        if (languageIdx > 0) {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, languageIdx, dataEnd);
        }
        if (databaseIdx > 0) {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, databaseIdx, dataEnd);
        }
        if (attachDbFileIdx > 0) {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, attachDbFileIdx, dataEnd);
        }
        if (changePasswordIdx > 0) {
            DataTypeWriter.encodeTwoByteIntegerLow(buffer, changePasswordIdx, dataEnd);
        }
        if (extensionsIdx > 0) {
            DataTypeWriter.encodeFourByteIntegerLow(buffer, extensionsIdx, idx + bufferOffset);
        }
        writer.writeBytes(buffer, 0, idx);
        if (extensionsIdx > 0) {
            for (Login7Feature feature : this.features) {
                feature.write(writer);
            }
            writer.writeByte((byte)-1);
        }
    }

    @Override
    public String getPacketType() {
        return "Login7";
    }

    @Override
    public String toString() {
        return "Login7: user: " + this.username + ", app: " + this.appName + ", client: " + this.clientName;
    }

    private String readString(byte[] bytes, int offset, int idx) {
        short off = DataTypeReader.readTwoByteIntegerLow(bytes, idx);
        short len = DataTypeReader.readTwoByteIntegerLow(bytes, idx += 2);
        idx += 2;
        if (len == 0) {
            return "";
        }
        return new String(bytes, offset + 8 + off, len * 2, StandardCharsets.UTF_16LE);
    }

    private int readFeature(byte[] bytes, int offset) {
        Login7Feature feature;
        int idx = offset;
        byte featureType = bytes[idx];
        ++idx;
        switch (featureType) {
            case 1: {
                feature = new Login7FeatureSessionRecovery();
                idx += feature.readFromBytes(bytes, idx);
                break;
            }
            case 4: {
                feature = new Login7FeatureColumnEncryption();
                idx += feature.readFromBytes(bytes, idx);
                break;
            }
            case 5: {
                feature = new Login7FeatureGlobalTransactions();
                idx += feature.readFromBytes(bytes, idx);
                break;
            }
            case 9: {
                feature = new Login7FeatureDataClassification();
                idx += feature.readFromBytes(bytes, idx);
                break;
            }
            case 10: {
                feature = new Login7FeatureUTF8Support();
                idx += feature.readFromBytes(bytes, idx);
                break;
            }
            case 11: {
                feature = new Login7FeatureAzureSQLDNSCaching();
                idx += feature.readFromBytes(bytes, idx);
                break;
            }
            default: {
                feature = new Login7FeatureUnknown();
                idx += feature.readFromBytes(bytes, idx - 1);
            }
        }
        this.features.add(feature);
        return idx - offset;
    }

    public int getTdsVersion() {
        return this.tdsVersion;
    }

    public void setTdsVersion(int tdsVersion) {
        this.tdsVersion = tdsVersion;
    }

    public int getPacketSize() {
        return this.packetSize;
    }

    public void setPacketSize(int packetSize) {
        this.packetSize = packetSize;
    }

    public byte[] getClientProgramVersion() {
        return this.clientProgramVersion;
    }

    public void setClientProgramVersion(byte[] clientProgramVersion) {
        this.clientProgramVersion = clientProgramVersion;
    }

    public long getClientPid() {
        return this.clientPid;
    }

    public void setClientPid(long clientPid) {
        this.clientPid = clientPid;
    }

    public long getConnectionPid() {
        return this.connectionPid;
    }

    public void setConnectionPid(long connectionPid) {
        this.connectionPid = connectionPid;
    }

    public boolean isfByteOrder() {
        return this.fByteOrder;
    }

    public void setfByteOrder(boolean fByteOrder) {
        this.fByteOrder = fByteOrder;
    }

    public boolean isfChar() {
        return this.fChar;
    }

    public void setfChar(boolean fChar) {
        this.fChar = fChar;
    }

    public byte getfFloat() {
        return this.fFloat;
    }

    public void setfFloat(byte fFloat) {
        this.fFloat = fFloat;
    }

    public boolean isfDumpLoad() {
        return this.fDumpLoad;
    }

    public void setfDumpLoad(boolean fDumpLoad) {
        this.fDumpLoad = fDumpLoad;
    }

    public boolean isfUseDB() {
        return this.fUseDB;
    }

    public void setfUseDB(boolean fUseDB) {
        this.fUseDB = fUseDB;
    }

    public boolean isfDatabase() {
        return this.fDatabase;
    }

    public void setfDatabase(boolean fDatabase) {
        this.fDatabase = fDatabase;
    }

    public boolean isfSetLang() {
        return this.fSetLang;
    }

    public void setfSetLang(boolean fSetLang) {
        this.fSetLang = fSetLang;
    }

    public boolean isfLanguage() {
        return this.fLanguage;
    }

    public void setfLanguage(boolean fLanguage) {
        this.fLanguage = fLanguage;
    }

    public boolean isfODBC() {
        return this.fODBC;
    }

    public void setfODBC(boolean fODBC) {
        this.fODBC = fODBC;
    }

    public boolean isfTranBoundary() {
        return this.fTranBoundary;
    }

    public void setfTranBoundary(boolean fTranBoundary) {
        this.fTranBoundary = fTranBoundary;
    }

    public boolean isfCacheConnect() {
        return this.fCacheConnect;
    }

    public void setfCacheConnect(boolean fCacheConnect) {
        this.fCacheConnect = fCacheConnect;
    }

    public byte getfUserType() {
        return this.fUserType;
    }

    public void setfUserType(byte fUserType) {
        this.fUserType = fUserType;
    }

    public boolean isfIntSecurity() {
        return this.fIntSecurity;
    }

    public void setfIntSecurity(boolean fIntSecurity) {
        this.fIntSecurity = fIntSecurity;
    }

    public byte getfSQLType() {
        return this.fSQLType;
    }

    public void setfSQLType(byte fSQLType) {
        this.fSQLType = fSQLType;
    }

    public boolean isfOLEDB() {
        return this.fOLEDB;
    }

    public void setfOLEDB(boolean fOLEDB) {
        this.fOLEDB = fOLEDB;
    }

    public boolean isfReadOnlyIntent() {
        return this.fReadOnlyIntent;
    }

    public void setfReadOnlyIntent(boolean fReadOnlyIntent) {
        this.fReadOnlyIntent = fReadOnlyIntent;
    }

    public boolean isfChangePassword() {
        return this.fChangePassword;
    }

    public void setfChangePassword(boolean fChangePassword) {
        this.fChangePassword = fChangePassword;
    }

    public boolean isfUserInstance() {
        return this.fUserInstance;
    }

    public void setfUserInstance(boolean fUserInstance) {
        this.fUserInstance = fUserInstance;
    }

    public boolean isfSendYukonBinaryXML() {
        return this.fSendYukonBinaryXML;
    }

    public void setfSendYukonBinaryXML(boolean fSendYukonBinaryXML) {
        this.fSendYukonBinaryXML = fSendYukonBinaryXML;
    }

    public boolean isfUnknownCollationHandling() {
        return this.fUnknownCollationHandling;
    }

    public void setfUnknownCollationHandling(boolean fUnknownCollationHandling) {
        this.fUnknownCollationHandling = fUnknownCollationHandling;
    }

    public boolean isfExtension() {
        return this.fExtension;
    }

    public void setfExtension(boolean fExtension) {
        this.fExtension = fExtension;
    }

    public int getClientTimeZone() {
        return this.clientTimeZone;
    }

    public void setClientTimeZone(int clientTimeZone) {
        this.clientTimeZone = clientTimeZone;
    }

    public int getClientLcid() {
        return this.clientLcid;
    }

    public void setClientLcid(int clientLcid) {
        this.clientLcid = clientLcid;
    }

    public String getHostname() {
        return this.hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAppName() {
        return this.appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getServerName() {
        return this.serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getClientName() {
        return this.clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getLanguage() {
        return this.language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getDatabase() {
        return this.database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public byte[] getClientId() {
        return this.clientId;
    }

    public void setClientId(byte[] clientId) {
        this.clientId = clientId;
    }

    public byte[] getSspi() {
        return this.sspi;
    }

    public void setSspi(byte[] sspi) {
        this.sspi = sspi;
    }

    public String getAttachDbFile() {
        return this.attachDbFile;
    }

    public void setAttachDbFile(String attachDbFile) {
        this.attachDbFile = attachDbFile;
    }

    public String getChangePassword() {
        return this.changePassword;
    }

    public void setChangePassword(String changePassword) {
        this.changePassword = changePassword;
    }

    public List<Login7Feature> getFeatures() {
        return this.features;
    }

    public void setFeatures(List<Login7Feature> features) {
        this.features = features;
    }

    public Login7Feature getFeature(String featureName) {
        if (featureName == null || featureName.trim().isEmpty()) {
            return null;
        }
        for (Login7Feature feature : this.features) {
            if (!featureName.equalsIgnoreCase(feature.getFeatureType())) continue;
            return feature;
        }
        return null;
    }

    public Login7Feature removeFeature(String featureName) {
        if (featureName == null || featureName.trim().isEmpty()) {
            return null;
        }
        Login7Feature foundFeature = null;
        for (Login7Feature feature : this.features) {
            if (!featureName.equalsIgnoreCase(feature.getFeatureType())) continue;
            foundFeature = feature;
            break;
        }
        if (foundFeature == null) {
            log.debug(Markers.MSSQL, "Login feature could not be removed because it was not found in the login packet: " + featureName);
        } else {
            this.features.remove(foundFeature);
        }
        return foundFeature;
    }

    public Login7Feature addFeature(String featureName) {
        Login7Feature newFeature = null;
        switch (featureName) {
            case "AzureSQLDNSCaching": {
                newFeature = new Login7FeatureAzureSQLDNSCaching();
                break;
            }
            case "ColumnEncryption": {
                newFeature = new Login7FeatureColumnEncryption();
                break;
            }
            case "DataClassification": {
                newFeature = new Login7FeatureDataClassification();
                break;
            }
            case "GlobalTransactions": {
                newFeature = new Login7FeatureGlobalTransactions();
                break;
            }
            case "SessionRecovery": {
                newFeature = new Login7FeatureSessionRecovery();
                break;
            }
            case "UTF8Support": {
                newFeature = new Login7FeatureUTF8Support();
                break;
            }
            default: {
                log.debug(Markers.MSSQL, "Login feature could not be added because its name is not valid: " + featureName);
                return null;
            }
        }
        this.features.add(newFeature);
        return newFeature;
    }

    @Override
    public Object getMember(String key) {
        switch (key) {
            case "tdsVersion": {
                return this.tdsVersion;
            }
            case "packetSize": {
                return this.packetSize;
            }
            case "clientProgramVersion": {
                return this.clientProgramVersion;
            }
            case "clientPid": {
                return this.clientPid;
            }
            case "connectionPid": {
                return this.connectionPid;
            }
            case "fByteOrder": {
                return this.fByteOrder;
            }
            case "fChar": {
                return this.fChar;
            }
            case "fFloat": {
                return this.fFloat;
            }
            case "fDumpLoad": {
                return this.fDumpLoad;
            }
            case "fUseDB": {
                return this.fUseDB;
            }
            case "fDatabase": {
                return this.fDatabase;
            }
            case "fSetLang": {
                return this.fSetLang;
            }
            case "fLanguage": {
                return this.fLanguage;
            }
            case "fODBC": {
                return this.fODBC;
            }
            case "fTranBoundary": {
                return this.fTranBoundary;
            }
            case "fCacheConnect": {
                return this.fCacheConnect;
            }
            case "fUserType": {
                return this.fUserType;
            }
            case "fIntSecurity": {
                return this.fIntSecurity;
            }
            case "fSQLType": {
                return this.fSQLType;
            }
            case "fOLEDB": {
                return this.fOLEDB;
            }
            case "fReadOnlyIntent": {
                return this.fReadOnlyIntent;
            }
            case "fChangePassword": {
                return this.fChangePassword;
            }
            case "fUserInstance": {
                return this.fUserInstance;
            }
            case "fSendYukonBinaryXML": {
                return this.fSendYukonBinaryXML;
            }
            case "fUnknownCollationHandling": {
                return this.fUnknownCollationHandling;
            }
            case "clientTimeZone": {
                return this.clientTimeZone;
            }
            case "clientLcid": {
                return this.clientLcid;
            }
            case "hostname": {
                return this.hostname;
            }
            case "username": {
                return this.username;
            }
            case "password": {
                return this.password;
            }
            case "appName": {
                return this.appName;
            }
            case "serverName": {
                return this.serverName;
            }
            case "clientName": {
                return this.clientName;
            }
            case "language": {
                return this.language;
            }
            case "database": {
                return this.database;
            }
            case "clientId": {
                return this.clientId;
            }
            case "sspi": {
                return this.sspi;
            }
            case "attachDbFile": {
                return this.attachDbFile;
            }
            case "changePassword": {
                return this.changePassword;
            }
            case "features": {
                return this.features;
            }
            case "getFeature": {
                return arg -> this.getFeature(arg[0].asString());
            }
            case "addFeature": {
                return arg -> this.addFeature(arg[0].asString());
            }
            case "removeFeature": {
                return arg -> this.removeFeature(arg[0].asString());
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
        return new String[]{"tdsVersion", "packetSize", "clientProgramVersion", "clientPid", "connectionPid", "fByteOrder", "fChar", "fFloat", "fDumpLoad", "fUseDB", "fDatabase", "fSetLang", "fLanguage", "fODBC", "fTranBoundary", "fCacheConnect", "fUserType", "fIntSecurity", "fSQLType", "fOLEDB", "fReadOnlyIntent", "fChangePassword", "fUserInstance", "fSendYukonBinaryXML", "fUnknownCollationHandling", "clientTimeZone", "clientLcid", "hostname", "username", "password", "appName", "serverName", "clientName", "language", "database", "clientId", "sspi", "attachDbFile", "changePassword", "features", "getFeature", "addFeature", "removeFeature", "remove", "toString"};
    }

    @Override
    public boolean hasMember(String key) {
        switch (key) {
            case "tdsVersion": 
            case "packetSize": 
            case "clientProgramVersion": 
            case "clientPid": 
            case "connectionPid": 
            case "fByteOrder": 
            case "fChar": 
            case "fFloat": 
            case "fDumpLoad": 
            case "fUseDB": 
            case "fDatabase": 
            case "fSetLang": 
            case "fLanguage": 
            case "fODBC": 
            case "fTranBoundary": 
            case "fCacheConnect": 
            case "fUserType": 
            case "fIntSecurity": 
            case "fSQLType": 
            case "fOLEDB": 
            case "fReadOnlyIntent": 
            case "fChangePassword": 
            case "fUserInstance": 
            case "fSendYukonBinaryXML": 
            case "fUnknownCollationHandling": 
            case "clientTimeZone": 
            case "clientLcid": 
            case "hostname": 
            case "username": 
            case "password": 
            case "appName": 
            case "serverName": 
            case "clientName": 
            case "language": 
            case "database": 
            case "clientId": 
            case "sspi": 
            case "attachDbFile": 
            case "changePassword": 
            case "features": 
            case "getFeature": 
            case "addFeature": 
            case "removeFeature": 
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
            case "tdsVersion": {
                this.setTdsVersion(value.asInt());
                break;
            }
            case "packetSize": {
                this.setPacketSize(value.asInt());
                break;
            }
            case "clientProgramVersion": {
                this.setClientProgramVersion((byte[])value.asHostObject());
                break;
            }
            case "clientPid": {
                this.setClientPid(value.asLong());
                break;
            }
            case "connectionPid": {
                this.setConnectionPid(value.asLong());
                break;
            }
            case "fByteOrder": {
                this.setfByteOrder(value.asBoolean());
                break;
            }
            case "fChar": {
                this.setfChar(value.asBoolean());
                break;
            }
            case "fFloat": {
                this.setfFloat(value.asByte());
                break;
            }
            case "fDumpLoad": {
                this.setfDumpLoad(value.asBoolean());
                break;
            }
            case "fUseDB": {
                this.setfUseDB(value.asBoolean());
                break;
            }
            case "fDatabase": {
                this.setfDatabase(value.asBoolean());
                break;
            }
            case "fSetLang": {
                this.setfSetLang(value.asBoolean());
                break;
            }
            case "fLanguage": {
                this.setfLanguage(value.asBoolean());
                break;
            }
            case "fODBC": {
                this.setfODBC(value.asBoolean());
                break;
            }
            case "fTranBoundary": {
                this.setfTranBoundary(value.asBoolean());
                break;
            }
            case "fCacheConnect": {
                this.setfCacheConnect(value.asBoolean());
                break;
            }
            case "fUserType": {
                this.setfUserType(value.asByte());
                break;
            }
            case "fIntSecurity": {
                this.setfIntSecurity(value.asBoolean());
                break;
            }
            case "fSQLType": {
                this.setfSQLType(value.asByte());
                break;
            }
            case "fOLEDB": {
                this.setfOLEDB(value.asBoolean());
                break;
            }
            case "fReadOnlyIntent": {
                this.setfReadOnlyIntent(value.asBoolean());
                break;
            }
            case "fChangePassword": {
                this.setfChangePassword(value.asBoolean());
                break;
            }
            case "fUserInstance": {
                this.setfUserInstance(value.asBoolean());
                break;
            }
            case "fSendYukonBinaryXML": {
                this.setfSendYukonBinaryXML(value.asBoolean());
                break;
            }
            case "fUnknownCollationHandling": {
                this.setfUnknownCollationHandling(value.asBoolean());
                break;
            }
            case "clientTimeZone": {
                this.setClientTimeZone(value.asInt());
                break;
            }
            case "clientLcid": {
                this.setClientLcid(value.asInt());
                break;
            }
            case "hostname": {
                this.setHostname(value.asString());
                break;
            }
            case "username": {
                this.setUsername(value.asString());
                break;
            }
            case "password": {
                this.setPassword(value.asString());
                break;
            }
            case "appName": {
                this.setAppName(value.asString());
                break;
            }
            case "serverName": {
                this.setServerName(value.asString());
                break;
            }
            case "clientName": {
                this.setClientName(value.asString());
                break;
            }
            case "language": {
                this.setLanguage(value.asString());
                break;
            }
            case "database": {
                this.setDatabase(value.asString());
                break;
            }
            case "clientId": {
                this.setClientId((byte[])value.asHostObject());
                break;
            }
            case "sspi": {
                this.setSspi((byte[])value.asHostObject());
                break;
            }
            case "attachDbFile": {
                this.setAttachDbFile(value.asString());
                break;
            }
            case "changePassword": {
                this.setChangePassword(value.asString());
                break;
            }
            default: {
                throw new ServerException("db.mssql.logic.NoSuchMember", key);
            }
        }
    }

    @Override
    public boolean removeMember(String key) {
        throw new ServerException("db.mssql.logic.CannotRemoveMember", key, "Login7 packet");
    }
}
