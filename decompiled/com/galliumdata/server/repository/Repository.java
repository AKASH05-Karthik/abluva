/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package com.galliumdata.server.repository;

import com.galliumdata.server.ServerException;
import com.galliumdata.server.repository.Connection;
import com.galliumdata.server.repository.Library;
import com.galliumdata.server.repository.LogSetting;
import com.galliumdata.server.repository.Persisted;
import com.galliumdata.server.repository.Project;
import com.galliumdata.server.repository.RepositoryException;
import com.galliumdata.server.repository.RepositoryObject;
import com.galliumdata.server.repository.RepositoryUtil;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Repository
extends RepositoryObject {
    private final Path rootDir;
    private static final String FILENAME = "repository.json";
    private static final Logger log = LogManager.getLogger((String)"galliumdata.core");
    @Persisted(JSONName="RepositoryVersion")
    protected String versionNum;
    @Persisted(directoryName="projects", fileName="project.json", memberClass=Project.class)
    protected Map<String, Project> projects;
    @Persisted(JSONName="libraries", memberClass=Library.class)
    protected Set<Library> libraries;
    @Persisted(JSONName="logSettings", memberClass=LogSetting.class)
    protected Set<LogSetting> logSettings;

    public Repository(String rootDir) {
        super(null);
        this.setRepository(this);
        if (null == rootDir) {
            throw new RepositoryException("repo.BadLocation", "null", "null");
        }
        this.rootDir = Paths.get(rootDir, new String[0]);
        if (!Files.exists(this.rootDir, new LinkOption[0])) {
            try {
                Files.createDirectories(this.rootDir, new FileAttribute[0]);
            }
            catch (Exception ex) {
                throw new ServerException("repo.CannotCreateRepo", rootDir, ex.getMessage());
            }
        }
        RepositoryUtil.checkDirectory(this.rootDir);
        this.path = this.rootDir.resolve(FILENAME);
        if (!Files.exists(this.path, new LinkOption[0])) {
            log.info("Repository does not exist at " + rootDir + ", creating it now");
            try {
                FileWriter fw = new FileWriter(this.path.toFile());
                fw.write("{\n  \"RepositoryVersion\": \"1.0\",\n  \"SystemSettings\": {},\n  \"libraries\": [],\n  \"logSettings\": [\n    {\n      \"loggerName\": \"core\",\n      \"level\": \"INFO\"\n    },\n    {\n      \"loggerName\": \"uselog\",\n      \"level\": \"INFO\"\n    },\n    {\n      \"loggerName\": \"rest\",\n      \"level\": \"INFO\"\n    },\n    {\n      \"loggerName\": \"dbproto\",\n      \"level\": \"INFO\"\n    },\n    {\n      \"loggerName\": \"ssl\",\n      \"level\": \"INFO\"\n    },\n    {\n      \"loggerName\": \"maven\",\n      \"level\": \"INFO\"\n    }\n  ]\n}");
                fw.close();
                Path projsPath = this.path.resolveSibling("projects");
                if (!Files.exists(projsPath, new LinkOption[0])) {
                    Files.createDirectory(projsPath, new FileAttribute[0]);
                }
            }
            catch (Exception ex) {
                throw new ServerException("repo.CannotCreateRepo", this.path.toString(), ex.getMessage());
            }
        }
        RepositoryUtil.checkFile(this.path);
        this.readFromJson();
    }

    @Override
    public String getName() {
        return "repository";
    }

    protected String getFileName() {
        return FILENAME;
    }

    public Path getRootDir() {
        return this.rootDir;
    }

    public String getVersionNum() {
        return this.versionNum;
    }

    public void setVersionNum(String versionNum) {
        this.versionNum = versionNum;
    }

    public Map<String, Project> getProjects() {
        if (this.projects == null) {
            this.projects = new TreeMap<String, Project>();
            try {
                super.readCollectionFromJSON(this.getClass().getDeclaredField("projects"));
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        for (Project project : this.projects.values()) {
            if (!project.isActive()) continue;
            for (Connection conn : project.getConnections().values()) {
                if (conn.isActive()) continue;
            }
        }
        return this.projects;
    }

    public Set<Library> getLibraries() {
        return this.libraries;
    }

    public Set<LogSetting> getLogSettings() {
        if (this.logSettings == null) {
            return new HashSet<LogSetting>();
        }
        return this.logSettings;
    }
}
