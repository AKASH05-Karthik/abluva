/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.logging.log4j.Logger
 *  org.graalvm.polyglot.Value
 *  org.graalvm.polyglot.proxy.ProxyObject
 */
package com.galliumdata.server.adapters;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.Logger;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

public class Variables
implements ProxyObject {
    private final Map<String, Object> values;

    public Variables() {
        this.values = new HashMap<String, Object>(30);
    }

    public Variables(Map<String, Object> model) {
        this.values = new HashMap<String, Object>(model);
    }

    public Logger getLog(String logName) {
        return (Logger)this.values.get(logName);
    }

    public int size() {
        return this.values.size();
    }

    public boolean isEmpty() {
        return this.values.isEmpty();
    }

    public boolean containsKey(Object key) {
        return this.values.containsKey(key);
    }

    public Object get(Object key) {
        return this.values.get(key);
    }

    public Object put(String name, Object value) {
        if (value == null) {
            return this.values.remove(name);
        }
        return this.values.put(name, value);
    }

    public void putAll(Variables vars) {
        for (String key : vars.keySet()) {
            this.values.put(key, vars.get(key));
        }
    }

    public Object remove(Object key) {
        return this.values.remove(key);
    }

    public void clear() {
        this.values.clear();
    }

    public Set<String> keySet() {
        return this.values.keySet();
    }

    public Collection<Object> values() {
        return this.values.values();
    }

    public void add(Object value) {
        for (int i = 0; i < 1000000000; ++i) {
            String key = "" + i;
            if (this.values.containsKey(key)) continue;
            this.values.put(key, value);
            return;
        }
        throw new RuntimeException("Array is full");
    }

    public String toString() {
        Object s = "";
        for (Map.Entry<String, Object> entry : this.values.entrySet()) {
            if (((String)s).length() > 0) {
                s = (String)s + ", ";
            }
            s = (String)s + entry.getKey();
            s = (String)s + "=";
            Object value = entry.getValue();
            if (value == null) {
                s = (String)s + "<null>";
                continue;
            }
            Object valueStr = value.toString();
            if (((String)valueStr).length() > 50) {
                valueStr = ((String)valueStr).substring(0, 50) + "...";
            }
            s = (String)s + ((String)valueStr).replaceAll("\\s", " ");
        }
        return s;
    }

    public Object getMember(String key) {
        return this.values.get(key);
    }

    public Object getMemberKeys() {
        return this.values.keySet().toArray();
    }

    public boolean hasMember(String key) {
        return this.values.containsKey(key);
    }

    public void putMember(String key, Value value) {
        this.values.put(key, value);
    }

    public boolean removeMember(String key) {
        if (!this.values.containsKey(key)) {
            return false;
        }
        this.values.remove(key);
        return true;
    }
}
