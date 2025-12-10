/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.repository;

public class Breakpoint {
    public String filename;
    public int linenum;

    public Breakpoint(String file, int line) {
        this.filename = file;
        this.linenum = line;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Breakpoint)) {
            return false;
        }
        Breakpoint bp = (Breakpoint)o;
        if (!this.filename.equals(bp.filename)) {
            return false;
        }
        return this.linenum == bp.linenum;
    }

    public int hashCode() {
        return this.filename.hashCode() + this.linenum;
    }
}
