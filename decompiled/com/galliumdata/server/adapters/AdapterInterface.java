/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.adapters;

import com.galliumdata.server.adapters.AdapterCallback;
import com.galliumdata.server.adapters.AdapterStatus;
import com.galliumdata.server.repository.Connection;
import com.galliumdata.server.repository.Project;

public interface AdapterInterface
extends Runnable {
    public void initialize();

    public boolean configure(Project var1, Connection var2, AdapterCallback var3);

    public void stopProcessing();

    public void switchProject(Project var1, Connection var2);

    public void shutdown();

    public AdapterStatus getStatus();

    public String getName();

    public String testConnection(Project var1, Connection var2);
}
