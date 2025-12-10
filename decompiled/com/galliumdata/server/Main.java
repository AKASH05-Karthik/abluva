/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.logging.log4j.Level
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 *  org.apache.logging.log4j.core.LoggerContext
 */
package com.galliumdata.server;

import com.galliumdata.server.ServerException;
import com.galliumdata.server.adapters.AdapterCallback;
import com.galliumdata.server.adapters.AdapterInterface;
import com.galliumdata.server.adapters.AdapterManager;
import com.galliumdata.server.log.Markers;
import com.galliumdata.server.repository.Connection;
import com.galliumdata.server.repository.Library;
import com.galliumdata.server.repository.LogSetting;
import com.galliumdata.server.repository.Project;
import com.galliumdata.server.repository.Repository;
import com.galliumdata.server.repository.RepositoryManager;
import com.galliumdata.server.rest.RestManager;
import com.galliumdata.server.security.KeyLoaderManager;
import com.galliumdata.server.settings.SettingName;
import com.galliumdata.server.settings.SettingsManager;
import com.galliumdata.telemetry.TelemetryNewRelicService;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

public class Main {
    public static final String VERSION_NUMBER = "1.9.3";
    public static final String BUILD_NUMBER = "2245";
    public static final String EDITION_NAME = "Community";
    private static final Logger log = LogManager.getLogger((String)"galliumdata.core");
    private static final Logger logDb = LogManager.getLogger((String)"galliumdata.dbproto");
    private static final Logger logLogic = LogManager.getLogger((String)"galliumdata.uselog");
    private static final Logger logRest = LogManager.getLogger((String)"galliumdata.rest");
    private static final Logger logSsl = LogManager.getLogger((String)"galliumdata.ssl");
    private static final Logger logNetwork = LogManager.getLogger((String)"galliumdata.network");
    private static final Logger logMaven = LogManager.getLogger((String)"galliumdata.maven");
    private static final Map<Connection, AdapterInterface> runningAdapters = new ConcurrentHashMap<Connection, AdapterInterface>();
    private static final Map<Connection, Thread> adapterThreads = new ConcurrentHashMap<Connection, Thread>();
    public static String uuid = "";
    private static final int MAX_RUNNING_ADAPTERS = 1000;
    public static final String INSTANCE_START_TIMESTAMP = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);

    public static void main(String[] args) {
        log.info(Markers.SYSTEM, "Gallium Data v.1.9.3 (build 2245) is now starting");
        System.setProperty("java.awt.headless", "true");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Gallium Data is shutting down, cleaning up...");
            Main.stopServer();
        }));
        SettingsManager.initialize(args);
        RestManager.startService();
        Main.startServer();
        log.info(Markers.SYSTEM, "Gallium Data is now running");
    }

    public static void startServer() {
        Repository mainRepo = RepositoryManager.getMainRepository();
        Map<String, Project> projects = mainRepo.getProjects();
        RepositoryManager.getLibrariesClassLoader();
        for (LogSetting setting : mainRepo.getLogSettings()) {
            LoggerContext logContext = (LoggerContext)LogManager.getContext((boolean)false);
            String loggerName = "galliumdata." + setting.getLoggerName();
            try {
                logContext.getConfiguration().getLoggerConfig(loggerName).setLevel(Level.getLevel((String)setting.getLevel()));
            }
            catch (Exception ex) {
                log.warn(Markers.SYSTEM, "Unable to set logging level specified in repository: " + String.valueOf(setting) + ", error is:" + ex.getMessage());
            }
        }
        Main.loadKeys(projects.values());
        for (Project project : projects.values()) {
            if (!project.isActive()) continue;
            Main.startProject(project);
        }
        Main.getSystemUUID();
        TelemetryNewRelicService.sendEventAsync("serverStart", "buildNumber", BUILD_NUMBER);
        TelemetryNewRelicService.startUptimeTelemetry();
    }

    private static void getSystemUUID() {
        String sep;
        String settingUUID = SettingsManager.getInstance().getStringSetting(SettingName.SYSTEM_UUID);
        if (settingUUID != null && !settingUUID.isBlank()) {
            uuid = settingUUID;
            return;
        }
        String tmpBase = System.getProperty("java.io.tmpdir");
        Path uuidPath = Paths.get(tmpBase + (sep = System.getProperty("file.separator")) + "galliumdata.uuid", new String[0]);
        if (Files.exists(uuidPath, new LinkOption[0]) && Files.isRegularFile(uuidPath, new LinkOption[0]) && Files.isReadable(uuidPath)) {
            try {
                byte[] bytes = Files.readAllBytes(uuidPath);
                uuid = UUID.fromString(new String(bytes)).toString();
            }
            catch (Exception bytes) {}
        } else {
            uuid = UUID.randomUUID().toString();
            try {
                Files.write(uuidPath, uuid.getBytes(), new OpenOption[0]);
            }
            catch (Exception ex) {
                log.debug(Markers.SYSTEM, "Unable to write UUID to " + String.valueOf(uuidPath) + ": " + ex.getMessage());
            }
        }
    }

    public static void switchRepository(Repository oldRepo) {
        log.debug(Markers.REPO, "Switching repository");
        Repository mainRepo = RepositoryManager.getMainRepository();
        Set<Library> newLibs = mainRepo.getLibraries();
        Set<Library> oldLibs = oldRepo.getLibraries();
        if (newLibs == null && oldLibs != null || newLibs != null && oldLibs == null) {
            RepositoryManager.resetLibrariesClassLoader();
        }
        if (oldLibs != null && newLibs != null && !oldLibs.equals(newLibs)) {
            RepositoryManager.resetLibrariesClassLoader();
        }
        HashSet<Project> deletedProjects = new HashSet<Project>();
        HashSet<Project> newProjects = new HashSet<Project>();
        for (Map.Entry<String, Project> entry : mainRepo.getProjects().entrySet()) {
            String string = entry.getKey();
            if (oldRepo.getProjects().containsKey(string)) continue;
            newProjects.add(entry.getValue());
        }
        for (Map.Entry<String, Project> entry : oldRepo.getProjects().entrySet()) {
            String string = entry.getKey();
            if (mainRepo.getProjects().containsKey(string)) continue;
            deletedProjects.add(entry.getValue());
        }
        HashMap<Connection, AdapterInterface> adaptersToRemove = new HashMap<Connection, AdapterInterface>();
        for (Project project : deletedProjects) {
            if (!project.isActive()) continue;
            log.trace(Markers.SYSTEM, "Stopping deleted project: " + project.getName());
            for (Map.Entry<String, Connection> conEntry : project.getConnections().entrySet()) {
                Connection conn = conEntry.getValue();
                if (!conn.isActive()) continue;
                AdapterInterface adapter = runningAdapters.get(conn);
                if (adapter == null) {
                    throw new RuntimeException("UNEXPECTED: AdapterInterface has disappeared after repo switch???");
                }
                adapter.shutdown();
                adaptersToRemove.put(conn, adapter);
            }
        }
        for (Map.Entry entry : adaptersToRemove.entrySet()) {
            Connection conn = (Connection)entry.getKey();
            AdapterInterface adapter = runningAdapters.get(conn);
            if (adapter == null) {
                throw new RuntimeException("UNEXPECTED: Project was deleted, but can't find its connection to close");
            }
            if (adapter != entry.getValue()) {
                throw new RuntimeException("UNEXPECTED: Project was deleted, but its adapter was not as expected");
            }
            runningAdapters.remove(conn);
            adapterThreads.remove(conn);
        }
        for (Map.Entry entry : oldRepo.getProjects().entrySet()) {
            AdapterInterface adapter;
            Connection conn;
            Connection newConn;
            String oldProjectName = (String)entry.getKey();
            Project oldProject = (Project)entry.getValue();
            if (deletedProjects.contains(oldProject)) continue;
            Project newProject = mainRepo.getProjects().get(oldProjectName);
            if (newProject == null) {
                throw new RuntimeException("UNEXPECTED: Project has disappeared???");
            }
            newProject.setProjectContext(oldProject.getProjectContext());
            HashSet<String> deletedConnections = new HashSet<String>();
            HashSet<String> changedConnections = new HashSet<String>();
            HashSet<String> unchangedConnections = new HashSet<String>();
            HashSet<String> newConnections = new HashSet<String>();
            for (Map.Entry<String, Connection> conEntry : oldProject.getConnections().entrySet()) {
                String oldConnectionName = conEntry.getKey();
                Connection oldConnection = conEntry.getValue();
                newConn = newProject.getConnections().get(oldConnectionName);
                if (newConn == null) {
                    if (!oldConnection.isActive()) continue;
                    deletedConnections.add(oldConnectionName);
                    continue;
                }
                if (!newConn.getParameters().equals(oldConnection.getParameters()) || newConn.isActive() != oldConnection.isActive()) {
                    changedConnections.add(oldConnectionName);
                    continue;
                }
                if (!oldConnection.isActive()) continue;
                unchangedConnections.add(oldConnectionName);
            }
            for (Map.Entry<String, Connection> conEntry : newProject.getConnections().entrySet()) {
                String newConnectionName = conEntry.getKey();
                if (oldProject.getConnections().containsKey(newConnectionName)) continue;
                newConnections.add(newConnectionName);
            }
            for (String connName : deletedConnections) {
                conn = oldProject.getConnections().get(connName);
                adapter = runningAdapters.get(conn);
                if (adapter == null) continue;
                adapter.shutdown();
                runningAdapters.remove(conn);
                adapterThreads.remove(conn);
            }
            if (oldProject.getCryptoHash() != newProject.getCryptoHash()) {
                changedConnections.addAll(newProject.getConnections().keySet());
            }
            for (String connName : changedConnections) {
                conn = oldProject.getConnections().get(connName);
                adapter = runningAdapters.get(conn);
                if (conn.isActive()) {
                    if (adapter == null) {
                        log.debug(Markers.SYSTEM, "Adapter not found, probably not running");
                    } else {
                        log.debug(Markers.REPO, "Shutting down [" + adapter.getName() + "]");
                        adapter.shutdown();
                        runningAdapters.remove(conn);
                        adapterThreads.remove(conn);
                    }
                }
                newConn = newProject.getConnections().get(connName);
                Main.startConnection(newProject, newConn);
            }
            for (String connName : unchangedConnections) {
                Connection oldConn = oldProject.getConnections().get(connName);
                Connection newConn2 = newProject.getConnections().get(connName);
                AdapterInterface adapter2 = runningAdapters.get(oldConn);
                if (adapter2 == null) {
                    log.debug("Running adapter for " + oldConn.getName() + " was not found during deployment, probably a secondary connection, ignoring");
                    continue;
                }
                runningAdapters.remove(oldConn);
                runningAdapters.put(newConn2, adapter2);
                Thread thread = adapterThreads.get(oldConn);
                adapterThreads.remove(oldConn);
                adapterThreads.put(newConn2, thread);
                adapter2.switchProject(newProject, newConn2);
            }
            for (String connName : newConnections) {
                conn = newProject.getConnections().get(connName);
                Main.startConnection(newProject, conn);
            }
            newProject.forgetEverything();
        }
        Main.loadKeys(newProjects);
        for (Project project : newProjects) {
            Main.startProject(project);
        }
    }

    public static void stopServer() {
        log.info("Stopping Gallium Data server");
        HashSet<Map.Entry<Connection, AdapterInterface>> adapterEntries = new HashSet<Map.Entry<Connection, AdapterInterface>>(runningAdapters.entrySet());
        for (Map.Entry entry : adapterEntries) {
            log.info(Markers.SYSTEM, "Stopping connection: " + ((Connection)entry.getKey()).getName());
            ((AdapterInterface)entry.getValue()).stopProcessing();
            ((AdapterInterface)entry.getValue()).shutdown();
            runningAdapters.remove(entry.getKey());
        }
        if (runningAdapters.size() > 0) {
            log.warn(Markers.SYSTEM, "There are running adapters after stopServer ???");
            throw new RuntimeException("There are running adapters after stopServer ???");
        }
        log.info("Gallium Data is now stopped");
    }

    protected static void startProject(Project project) {
        for (Connection conn : project.getConnections().values()) {
            Main.startConnection(project, conn);
        }
    }

    protected static void startConnection(Project project, Connection conn) {
        AdapterCallback callback;
        if (!conn.isActive()) {
            return;
        }
        if (runningAdapters.size() > 1000) {
            throw new ServerException("core.TooManyAdapters", runningAdapters.size(), 1000);
        }
        String adapterType = conn.getAdapterType();
        AdapterInterface adapter = AdapterManager.getInstance().instantiateAdapter(adapterType);
        if (adapter.configure(project, conn, callback = new AdapterCallback(project))) {
            runningAdapters.put(conn, adapter);
            Thread thread = new Thread(adapter);
            thread.setName("Listener for " + conn.getName());
            adapterThreads.put(conn, thread);
            thread.start();
        }
    }

    public static Map<Connection, AdapterInterface> getRunningAdapters() {
        return runningAdapters;
    }

    private static void loadKeys(Collection<Project> projects) {
        KeyLoaderManager keyLoaderMgr = new KeyLoaderManager();
        keyLoaderMgr.loadKeyLoaders();
        KeyManager[] keyManagers = keyLoaderMgr.getKeyManagers();
        TrustManager[] trustManagers = keyLoaderMgr.getTrustManagers();
        if (keyManagers != null || trustManagers != null) {
            for (Project project : projects) {
                if (!project.isActive()) continue;
                if (keyManagers != null) {
                    project.addKeyManagers(keyManagers);
                }
                if (trustManagers == null) continue;
                project.addTrustManagers(trustManagers);
            }
        }
    }
}
