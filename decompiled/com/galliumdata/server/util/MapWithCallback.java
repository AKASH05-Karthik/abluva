/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MapWithCallback<K, V>
extends HashMap<K, V> {
    private final Map<K, V> myMap;
    private final Runnable cb;

    public MapWithCallback(Map<K, V> map, Runnable cb) {
        this.myMap = map;
        this.cb = cb;
    }

    @Override
    public int size() {
        return this.myMap.size();
    }

    @Override
    public boolean isEmpty() {
        return this.myMap.isEmpty();
    }

    @Override
    public V get(Object key) {
        return this.myMap.get(key);
    }

    @Override
    public boolean containsKey(Object key) {
        return this.myMap.containsKey(key);
    }

    @Override
    public V put(K key, V value) {
        if (this.cb != null) {
            this.cb.run();
        }
        return this.myMap.put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        if (this.cb != null) {
            this.cb.run();
        }
        this.myMap.putAll(m);
    }

    @Override
    public V remove(Object key) {
        if (this.cb != null) {
            this.cb.run();
        }
        return this.myMap.remove(key);
    }

    @Override
    public void clear() {
        if (this.cb != null) {
            this.cb.run();
        }
        this.myMap.clear();
    }

    @Override
    public boolean containsValue(Object value) {
        return this.myMap.containsValue(value);
    }

    @Override
    public Set<K> keySet() {
        return Collections.unmodifiableSet(this.myMap.keySet());
    }

    @Override
    public Collection<V> values() {
        return Collections.unmodifiableCollection(this.myMap.values());
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return Collections.unmodifiableSet(this.myMap.entrySet());
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        return this.myMap.getOrDefault(key, defaultValue);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        V result = this.myMap.putIfAbsent(key, value);
        if (result != null && this.cb != null) {
            this.cb.run();
        }
        return result;
    }

    @Override
    public boolean remove(Object key, Object value) {
        if (this.cb != null) {
            this.cb.run();
        }
        return this.myMap.remove(key, value);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        if (this.cb != null) {
            this.cb.run();
        }
        return this.myMap.replace(key, oldValue, newValue);
    }

    @Override
    public V replace(K key, V value) {
        if (this.cb != null) {
            this.cb.run();
        }
        return this.myMap.replace(key, value);
    }
}
