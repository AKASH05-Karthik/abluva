/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server;

import java.io.IOException;

public interface Zippable {
    public byte[] zip() throws IOException;

    public String getName();
}
