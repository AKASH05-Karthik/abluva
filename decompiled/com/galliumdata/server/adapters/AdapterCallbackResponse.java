/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.adapters;

import java.util.ArrayList;
import java.util.List;

public class AdapterCallbackResponse {
    public boolean reject = false;
    public Object response;
    public boolean skip;
    public String errorMessage;
    public byte[] errorResponse;
    public long errorCode = 0L;
    public List<String> errorParameters = new ArrayList<String>();
    public int sqlStatus = 0;
    public boolean closeConnection = false;
    public String connectionName;
    public String logicName;
    public boolean doNotCall;
}
