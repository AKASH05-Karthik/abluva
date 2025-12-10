/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server;

import com.galliumdata.server.locale.I18nManager;

public class ServerException
extends RuntimeException {
    protected String msgName;
    protected Object[] args;

    public ServerException(String msgName, Object ... args) {
        super(msgName);
        this.msgName = msgName;
        this.args = args;
    }

    @Override
    public String getMessage() {
        return I18nManager.getString(this.msgName, this.args);
    }

    public Object[] getArgs() {
        return this.args;
    }

    public static void throwException(String errName) {
        String msg = I18nManager.getString(errName, new Object[0]);
        throw new ServerException(msg, new Object[0]);
    }
}
