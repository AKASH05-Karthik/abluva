/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.repository;

import com.galliumdata.server.adapters.Variables;
import com.galliumdata.server.repository.Persisted;
import com.galliumdata.server.repository.Repository;
import com.galliumdata.server.repository.RepositoryObject;
import java.util.HashMap;
import java.util.Map;

public class FilterUse
extends RepositoryObject {
    @Persisted(JSONName="filterType")
    protected String filterType;
    @Persisted(JSONName="phase")
    protected String phase;
    @Persisted(JSONName="priority")
    protected int priority;
    @Persisted(JSONName="parameters")
    protected Map<String, Object> parameters = new HashMap<String, Object>();
    private final Variables filterContext = new Variables();

    public FilterUse(Repository repo) {
        super(repo);
        this.parameters = new HashMap<String, Object>();
    }

    public String getFilterType() {
        return this.filterType;
    }

    public String getPhase() {
        return this.phase;
    }

    public int getPriority() {
        return this.priority;
    }

    public Map<String, Object> getParameters() {
        return this.parameters;
    }

    public boolean getBooleanParameter(String name) {
        Object obj = this.parameters.get(name);
        if (obj == null) {
            return false;
        }
        return (Boolean)obj;
    }

    public Variables getFilterContext() {
        return this.filterContext;
    }
}
