/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.handler.mssql;

public enum MSSQLPacketType {
    Attention,
    BulkLoadBCP,
    DataStream,
    FederatedAuthentication,
    Login7,
    Message,
    PreLogin,
    RPC,
    SQLBatch,
    SSPI,
    Unknown,
    ColInfo,
    ColMetadata,
    DataClassification,
    Done,
    DoneInProc,
    DoneProc,
    EnvChange,
    Error,
    FeatureExtAck,
    Info,
    LoginAck,
    NBCRow,
    Order,
    ReturnStatus,
    ReturnValue,
    Row,
    RowBatch,
    SessionState,
    SSPIToken,
    TabName;

}
