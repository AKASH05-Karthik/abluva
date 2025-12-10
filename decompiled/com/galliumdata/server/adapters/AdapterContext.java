/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.adapters;

import com.galliumdata.server.adapters.AdapterCallback;
import java.util.Map;
import java.util.TreeMap;

public class AdapterContext {
    public Map<String, Object> parameterValues = new TreeMap<String, Object>();
    public AdapterCallback callbacks;
}
