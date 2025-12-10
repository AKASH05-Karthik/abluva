/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.adapters;

import java.time.ZonedDateTime;

public class AdapterStatus {
    public ZonedDateTime startTime;
    public long numRequests;
    public long numRequestBytes;
    public long numResponses;
    public long numResponseBytes;
    public long numErrors;

    public synchronized void incrementNumRequests(long num) {
        this.numRequests += num;
    }

    public synchronized void incrementNumRequestBytes(long num) {
        this.numRequestBytes += num;
    }

    public synchronized void incrementNumResponses(long num) {
        this.numResponses += num;
    }

    public synchronized void incrementNumResponseBytes(long num) {
        this.numResponseBytes += num;
    }

    public synchronized void incrementNumErrors(long num) {
        this.numErrors += num;
    }
}
