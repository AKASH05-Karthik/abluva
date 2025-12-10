/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.repository;

import com.galliumdata.server.repository.Persisted;
import com.galliumdata.server.repository.Project;
import com.galliumdata.server.repository.Repository;
import com.galliumdata.server.repository.RepositoryException;
import com.galliumdata.server.repository.RepositoryObject;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Map;

public class Connection
extends RepositoryObject {
    @Persisted(JSONName="adapterType")
    protected String adapterType;
    @Persisted(JSONName="adapterVersion")
    protected String adapterVersion;
    @Persisted(JSONName="parameters")
    protected Map<String, Object> parameters;
    @Persisted(JSONName="keystorePassword")
    protected String keystorePassword;
    protected KeyStore keystore;

    public Connection(Repository repo) {
        super(repo);
    }

    public Project getProject() {
        return (Project)this.parentObject;
    }

    public String getAdapterType() {
        return this.adapterType;
    }

    public String getAdapterVersion() {
        return this.adapterVersion;
    }

    public Map<String, Object> getParameters() {
        return this.parameters;
    }

    public Object getParameterValue(String name) {
        if (null == this.parameters) {
            return null;
        }
        return this.parameters.get(name);
    }

    public KeyStore getKeystore() {
        if (this.keystore != null) {
            return this.keystore;
        }
        Path ksPath = this.path.resolveSibling("keystore.jks");
        if (!Files.exists(ksPath, new LinkOption[0])) {
            return null;
        }
        if (!Files.isReadable(ksPath)) {
            throw new RepositoryException("repo.FileNotReadable", ksPath);
        }
        try {
            this.keystore = KeyStore.getInstance("JKS");
            char[] pwChars = null;
            if (this.keystorePassword != null) {
                pwChars = this.keystorePassword.toCharArray();
            }
            this.keystore.load(Files.newInputStream(ksPath, new OpenOption[0]), pwChars);
        }
        catch (Exception ex) {
            throw new RepositoryException("repo.BadKeystore", ksPath, ex.getMessage());
        }
        return this.keystore;
    }

    public String getKeystorePassword() {
        return this.keystorePassword;
    }
}
