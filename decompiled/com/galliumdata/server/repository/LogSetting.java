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

public class LogSetting
extends RepositoryObject {
    @Persisted(JSONName="loggerName")
    protected String loggerName;
    @Persisted(JSONName="level")
    protected String level;

    public LogSetting(Repository repo, JsonNode node) {
        super(repo);
        if (!node.has("loggerName")) {
            throw new RepositoryException("repo.InvalidPropertyValue", "logSettings", "loggerName (missing)", "repository");
        }
        if (!node.has("level")) {
            throw new RepositoryException("repo.InvalidPropertyValue", "logSettings", "level (missing)", "repository");
        }
        this.loggerName = node.get("loggerName").asText();
        this.level = node.get("level").asText();
        if (this.loggerName == null || this.loggerName.trim().length() == 0) {
            throw new RepositoryException("repo.InvalidPropertyValue", "logSettings", "loggerName", "repository");
        }
        if (this.level == null || this.level.trim().length() == 0) {
            throw new RepositoryException("repo.InvalidPropertyValue", "logSettings", "level", "repository");
        }
    }

    public String getLoggerName() {
        return this.loggerName;
    }

    public String getLevel() {
        return this.level;
    }

    public int hashCode() {
        return this.loggerName.hashCode() + this.level.hashCode();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof LogSetting)) {
            return false;
        }
        LogSetting lib = (LogSetting)obj;
        return this.loggerName.equals(lib.loggerName) && this.level.equals(lib.level);
    }

    public String toString() {
        return "LogSetting " + this.loggerName + ":" + this.level;
    }
}
