/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.handler.mssql;

import com.galliumdata.server.handler.mssql.ConnectionState;
import com.galliumdata.server.handler.mssql.MSSQLPacket;
import com.galliumdata.server.handler.mssql.RawPacketReader;
import com.galliumdata.server.handler.mssql.RawPacketWriter;
import com.galliumdata.server.handler.mssql.UnableToParseException;
import com.galliumdata.server.handler.mssql.tokens.MessageToken;
import com.galliumdata.server.handler.mssql.tokens.TokenColMetadata;
import com.galliumdata.server.handler.mssql.tokens.TokenDataClassification;
import com.galliumdata.server.handler.mssql.tokens.TokenRow;
import java.util.ArrayList;
import java.util.List;

public class MessagePacket
extends MSSQLPacket {
    private List<MessageToken> tokens = new ArrayList<MessageToken>();
    private TokenColMetadata currentMetadata;
    private TokenDataClassification currentDataClassification;

    public MessagePacket(ConnectionState connectionState) {
        super(connectionState);
        this.typeCode = (byte)4;
    }

    @Override
    public int readFromBytes(byte[] bytes, int offset, int numBytes) {
        int idx = offset;
        idx += super.readFromBytes(bytes, offset, numBytes);
        while (idx - offset < numBytes && idx - offset < this.length) {
            MessageToken token = MessageToken.createToken(bytes[idx], this.connectionState);
            if (token.getTokenType() == -47 || token.getTokenType() == -46) {
                ((TokenRow)token).setColumnMetadata(this.currentMetadata.getColumns());
            }
            idx += token.readFromBytes(bytes, idx, numBytes - (idx - offset));
            this.tokens.add(token);
            if (token.getTokenType() != -127) continue;
            this.currentMetadata = (TokenColMetadata)token;
        }
        return idx - offset;
    }

    @Override
    public void read(RawPacketReader reader) {
        super.read(reader);
    }

    public MessageToken readNextToken(RawPacketReader reader) {
        if (reader.isDone()) {
            return null;
        }
        byte tokenType = reader.readByte();
        MessageToken token = MessageToken.createToken(tokenType, this.connectionState);
        if (token.getTokenType() == -47 || token.getTokenType() == -46) {
            if (this.currentMetadata == null) {
                throw new UnableToParseException("Row/NBCRow", "Mo current metadata");
            }
            ((TokenRow)token).setColumnMetadata(this.currentMetadata.getColumns());
            ((TokenRow)token).setDataClassification(this.currentDataClassification);
        }
        token.read(reader);
        if (token.getTokenType() == -127) {
            this.currentMetadata = (TokenColMetadata)token;
            this.currentDataClassification = null;
        }
        if (token.getTokenType() == -93) {
            this.currentDataClassification = (TokenDataClassification)token;
        }
        return token;
    }

    @Override
    public int getSerializedSize() {
        int size = super.getSerializedSize();
        for (MessageToken token : this.tokens) {
            size += token.getSerializedSize();
        }
        return size;
    }

    @Override
    public void write(RawPacketWriter writer) {
        super.write(writer);
        this.tokens.forEach(t -> t.write(writer));
    }

    @Override
    public String getPacketType() {
        return "Message";
    }

    public List<MessageToken> getTokens() {
        return this.tokens;
    }

    public void setTokens(List<MessageToken> tokens) {
        this.tokens = tokens;
    }
}
