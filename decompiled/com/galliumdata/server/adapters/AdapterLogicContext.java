/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.adapters;

import java.util.HashMap;
import java.util.Map;

public class AdapterLogicContext {
    private Map<String, Object> variables = new HashMap<String, Object>();

    public Object get(String name) {
        return this.variables.get(name);
    }

    public Object put(String name, Object value) {
        return this.variables.put(name, value);
    }
}
