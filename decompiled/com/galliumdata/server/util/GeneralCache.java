/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GeneralCache {
    private final Map<String, Entry> map = new ConcurrentHashMap<String, Entry>();
    private final int maxSize;
    private final long maxIdleTime;
    private long lastMaintenanceTime = System.currentTimeMillis();
    private static final long MAINTENANCE_INTERVAL = 5000L;

    public GeneralCache(int maxSize, long maxIdleTimeInSecs) {
        this.maxSize = maxSize;
        this.maxIdleTime = maxIdleTimeInSecs * 1000L;
    }

    public int getSize() {
        this.maintainCache();
        return this.map.size();
    }

    public boolean isEmpty() {
        this.maintainCache();
        return this.map.isEmpty();
    }

    public boolean containsKey(String key) {
        return this.map.containsKey(key);
    }

    public Collection<String> getKeys() {
        return this.map.keySet();
    }

    public void put(String key, Object value) {
        this.maintainCache();
        if (this.map.size() >= this.maxSize) {
            this.shrinkCache(this.maxSize / 10);
        }
        Entry entry = new Entry();
        entry.key = key;
        entry.value = value;
        this.map.put(key, entry);
    }

    public Object get(String key) {
        this.maintainCache();
        Entry entry = this.map.get(key);
        if (entry == null) {
            return null;
        }
        entry.lastAccessTime = System.currentTimeMillis();
        return entry.value;
    }

    public Object remove(String key) {
        this.maintainCache();
        Entry entry = this.map.remove(key);
        if (entry == null) {
            return null;
        }
        return entry.value;
    }

    private synchronized void maintainCache() {
        if (System.currentTimeMillis() - this.lastMaintenanceTime < 5000L) {
            return;
        }
        long minAccessTime = System.currentTimeMillis() - this.maxIdleTime;
        HashSet<String> toRemove = new HashSet<String>();
        for (Map.Entry<String, Entry> entry : this.map.entrySet()) {
            Entry ent = entry.getValue();
            if (ent.lastAccessTime >= minAccessTime) continue;
            toRemove.add(entry.getKey());
        }
        for (String rem : toRemove) {
            this.map.remove(rem);
        }
        this.lastMaintenanceTime = System.currentTimeMillis();
    }

    private synchronized void shrinkCache(int numToShrink) {
        ArrayList<Entry> entriesByTime = new ArrayList<Entry>(this.map.values());
        entriesByTime.sort(Comparator.comparingLong(e -> e.lastAccessTime));
        for (int i = 0; i < numToShrink; ++i) {
            this.map.remove(((Entry)entriesByTime.get((int)i)).key);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Entry> entry : this.map.entrySet()) {
            sb.append(entry.getKey());
            sb.append("=");
            sb.append(entry.getValue().value.toString());
            sb.append("\n");
        }
        return sb.toString();
    }

    private static class Entry {
        private long lastAccessTime = System.currentTimeMillis();
        private String key;
        private Object value;

        private Entry() {
        }
    }
}
