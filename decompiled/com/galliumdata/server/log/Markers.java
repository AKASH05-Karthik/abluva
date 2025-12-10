/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.logging.log4j.Marker
 *  org.apache.logging.log4j.MarkerManager$Log4jMarker
 */
package com.galliumdata.server.log;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public class Markers {
    public static final Marker SYSTEM = new MarkerManager.Log4jMarker("Sys");
    public static final Marker REPO = new MarkerManager.Log4jMarker("Repos");
    public static final Marker WEB = new MarkerManager.Log4jMarker("Web");
    public static final Marker MYSQL = new MarkerManager.Log4jMarker("MySQL");
    public static final Marker MONGO = new MarkerManager.Log4jMarker("MongoDB");
    public static final Marker POSTGRES = new MarkerManager.Log4jMarker("Postgr");
    public static final Marker VERTICA = new MarkerManager.Log4jMarker("Vertica");
    public static final Marker DNS = new MarkerManager.Log4jMarker("Dns");
    public static final Marker MSSQL = new MarkerManager.Log4jMarker("MSSQL");
    public static final Marker ORACLE = new MarkerManager.Log4jMarker("Oracle");
    public static final Marker REDIS = new MarkerManager.Log4jMarker("Redis");
    public static final Marker HTTP = new MarkerManager.Log4jMarker("HTTP");
    public static final Marker CASSANDRA = new MarkerManager.Log4jMarker("Cassandra");
    public static final Marker DB2 = new MarkerManager.Log4jMarker("DB2");
    public static final Marker USER_LOGIC = new MarkerManager.Log4jMarker("Logic");
}
