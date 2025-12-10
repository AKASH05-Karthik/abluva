/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.repository;

import com.galliumdata.server.repository.Persisted;
import com.galliumdata.server.repository.Repository;
import com.galliumdata.server.repository.RepositoryObject;

public class PortSetting
extends RepositoryObject {
    @Persisted(JSONName="PortNumber")
    private int portNumber;
    @Persisted(JSONName="Active")
    private boolean active;

    public PortSetting(Repository repo) {
        super(repo);
    }
}
