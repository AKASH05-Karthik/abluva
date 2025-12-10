/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.util;

public class Holder<T> {
    private T value;

    public Holder(T value) {
        this.value = value;
    }

    public T getValue() {
        return this.value;
    }

    public void setValue(T newValue) {
        this.value = newValue;
    }
}
