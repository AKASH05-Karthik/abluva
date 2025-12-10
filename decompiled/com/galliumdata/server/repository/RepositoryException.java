/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.repository;

import com.galliumdata.server.locale.I18nManager;

public class RepositoryException
extends RuntimeException {
    private String msgName;
    private Object[] args;

    public RepositoryException(String msgName, Object ... args) {
        super(msgName);
        this.msgName = msgName;
        this.args = args;
    }

    @Override
    public String getMessage() {
        return I18nManager.getString(this.msgName, this.args);
    }
}
