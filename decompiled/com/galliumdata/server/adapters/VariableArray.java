/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.graalvm.polyglot.Value
 *  org.graalvm.polyglot.proxy.ProxyArray
 */
package com.galliumdata.server.adapters;

import java.util.ArrayList;
import java.util.List;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;

public class VariableArray
implements ProxyArray {
    private final List<Object> objects = new ArrayList<Object>();

    public void add(Object obj) {
        this.objects.add(obj);
    }

    public void clear() {
        this.objects.clear();
    }

    public Object get(long index) {
        return this.objects.get((int)index);
    }

    public void set(long index, Value value) {
        if (value == null || value.isNull()) {
            this.objects.set((int)index, null);
        } else {
            this.objects.set((int)index, value);
        }
    }

    public boolean remove(long index) {
        return this.objects.remove(index);
    }

    public long getSize() {
        return this.objects.size();
    }
}
