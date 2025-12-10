/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.repository;

import com.galliumdata.server.repository.Repository;
import com.galliumdata.server.repository.RepositoryObject;

public class NullClass
extends RepositoryObject {
    public NullClass(Repository repo) {
        super(repo);
        throw new RuntimeException("This should never happen");
    }
}
