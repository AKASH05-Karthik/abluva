/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.util;

import java.util.AbstractSequentialList;
import java.util.List;
import java.util.ListIterator;

public class ListWithCallback<T>
extends AbstractSequentialList<T> {
    private final List<T> myList;
    private final Runnable cb;

    public ListWithCallback(List<T> list, Runnable cb) {
        this.myList = list;
        this.cb = cb;
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return new ListIterator<T>(){
            private int pos = -1;

            @Override
            public boolean hasNext() {
                if (ListWithCallback.this.myList == null) {
                    return false;
                }
                return this.pos < ListWithCallback.this.myList.size() - 1;
            }

            @Override
            public T next() {
                return ListWithCallback.this.myList.get(++this.pos);
            }

            @Override
            public boolean hasPrevious() {
                return this.pos > 0;
            }

            @Override
            public T previous() {
                return ListWithCallback.this.myList.get(this.pos - 1);
            }

            @Override
            public int nextIndex() {
                return this.pos + 1;
            }

            @Override
            public int previousIndex() {
                return this.pos - 1;
            }

            @Override
            public void remove() {
                ListWithCallback.this.myList.remove(this.pos);
                if (ListWithCallback.this.cb != null) {
                    ListWithCallback.this.cb.run();
                }
            }

            @Override
            public void set(T o) {
                ListWithCallback.this.myList.set(this.pos, o);
                if (ListWithCallback.this.cb != null) {
                    ListWithCallback.this.cb.run();
                }
            }

            @Override
            public void add(T o) {
                ListWithCallback.this.myList.add(o);
                if (ListWithCallback.this.cb != null) {
                    ListWithCallback.this.cb.run();
                }
            }
        };
    }

    @Override
    public int size() {
        if (this.myList == null) {
            return 0;
        }
        return this.myList.size();
    }

    @Override
    public T get(int idx) {
        return this.myList.get(idx);
    }

    @Override
    public T set(int idx, T obj) {
        if (this.cb != null) {
            this.cb.run();
        }
        return this.myList.set(idx, obj);
    }

    @Override
    public T remove(int idx) {
        if (this.cb != null) {
            this.cb.run();
        }
        return this.myList.remove(idx);
    }

    @Override
    public void add(int idx, T obj) {
        if (this.cb != null) {
            this.cb.run();
        }
        this.myList.add(idx, obj);
    }

    public void push(T obj) {
        if (this.cb != null) {
            this.cb.run();
        }
        this.myList.add(obj);
    }

    @Override
    public int indexOf(Object member) {
        return this.myList.indexOf(member);
    }
}
