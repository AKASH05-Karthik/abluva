/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.repository;

public enum FilterStage {
    CONNECTION("connection"),
    REQUEST("request"),
    RESPONSE("response"),
    DUPLEX("duplex");

    private String name;

    private FilterStage(String s) {
        this.name = s;
    }
}
