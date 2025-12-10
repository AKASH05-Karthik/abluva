/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.JsonNode
 */
package com.galliumdata.server.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.galliumdata.server.repository.Persisted;
import com.galliumdata.server.repository.Repository;
import com.galliumdata.server.repository.RepositoryException;
import com.galliumdata.server.repository.RepositoryObject;

public class Library
extends RepositoryObject {
    @Persisted(JSONName="type")
    protected String type;
    @Persisted(JSONName="orgId")
    protected String orgId;
    @Persisted(JSONName="artifactId")
    protected String artifactId;
    @Persisted(JSONName="version")
    protected String version;

    public Library(Repository repo, JsonNode node) {
        super(repo);
        if (!node.has("type")) {
            throw new RepositoryException("repo.InvalidPropertyValue", "libraries", "type (missing)", "repository");
        }
        if (!node.has("orgId")) {
            throw new RepositoryException("repo.InvalidPropertyValue", "libraries", "orgId (missing)", "repository");
        }
        if (!node.has("artifactId")) {
            throw new RepositoryException("repo.InvalidPropertyValue", "libraries", "artifactId (missing)", "repository");
        }
        if (!node.has("version")) {
            throw new RepositoryException("repo.InvalidPropertyValue", "libraries", "version (missing)", "repository");
        }
        this.type = node.get("type").asText();
        this.orgId = node.get("orgId").asText();
        this.artifactId = node.get("artifactId").asText();
        this.version = node.get("version").asText();
        if (this.type == null || this.type.trim().length() == 0) {
            throw new RepositoryException("repo.InvalidPropertyValue", "libraries", "type", "repository");
        }
        if (this.orgId == null || this.orgId.trim().length() == 0) {
            throw new RepositoryException("repo.InvalidPropertyValue", "libraries", "orgId", "repository");
        }
        if (this.artifactId == null || this.artifactId.trim().length() == 0) {
            throw new RepositoryException("repo.InvalidPropertyValue", "libraries", "artifactId", "repository");
        }
        if (this.version == null || this.version.trim().length() == 0) {
            throw new RepositoryException("repo.InvalidPropertyValue", "libraries", "version", "repository");
        }
    }

    public int hashCode() {
        return this.orgId.hashCode() + this.artifactId.hashCode() + this.version.hashCode();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Library)) {
            return false;
        }
        Library lib = (Library)obj;
        return this.orgId.equals(lib.orgId) && this.artifactId.equals(lib.artifactId) && this.version.equals(lib.version);
    }

    public String toString() {
        return "Library " + this.orgId + "/" + this.artifactId + "/" + this.version;
    }
}
