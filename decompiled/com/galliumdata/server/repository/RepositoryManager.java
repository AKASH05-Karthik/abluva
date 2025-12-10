/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package com.galliumdata.server.repository;

import com.galliumdata.server.Main;
import com.galliumdata.server.log.Markers;
import com.galliumdata.server.logic.ScriptExecutor;
import com.galliumdata.server.logic.ScriptManager;
import com.galliumdata.server.maven.IvyRepository;
import com.galliumdata.server.metarepo.MetaRepositoryManager;
import com.galliumdata.server.npm.NPMManager;
import com.galliumdata.server.repository.Library;
import com.galliumdata.server.repository.Repository;
import com.galliumdata.server.repository.RepositoryException;
import com.galliumdata.server.settings.SettingName;
import com.galliumdata.server.settings.SettingsManager;
import com.galliumdata.telemetry.TelemetryNewRelicService;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.AccessDeniedException;
import java.nio.file.CopyOption;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RepositoryManager {
    private static Repository mainRepo = null;
    private static ClassLoader librariesClassLoader;
    private static boolean librariesClassLoaderInitialized;
    private static final Logger log;

    public static Repository getMainRepository() {
        if (mainRepo != null) {
            return mainRepo;
        }
        String repoLoc = SettingsManager.getInstance().getStringSetting(SettingName.REPO_LOCATION);
        if (null == repoLoc) {
            throw new RepositoryException("repo.BadLocation", "null", "Must be specified");
        }
        mainRepo = new Repository(repoLoc);
        return mainRepo;
    }

    public static void installNewRepository(ZipInputStream zis) {
        Repository oldRepo = RepositoryManager.getMainRepository();
        Path backupDir = RepositoryManager.createBackupDirectory();
        if (backupDir != null) {
            RepositoryManager.copyRepositoryToBackup(backupDir);
        }
        RepositoryManager.deleteRepository();
        try {
            RepositoryManager.unzipRepository(zis);
        }
        catch (IOException ioex) {
            throw new RepositoryException("repo.backup.ErrorInstallingRepository", ioex.getMessage());
        }
        mainRepo = null;
        Main.switchRepository(oldRepo);
        ScriptManager.getInstance().forgetAllScripts();
        ScriptExecutor.forgetAllScripts();
        MetaRepositoryManager.getMainRepository().forgetFilterTypes();
        try {
            TelemetryNewRelicService.sendEventAsync("serverEvent", "repository", "publish");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static ClassLoader getLibrariesClassLoader() {
        if (librariesClassLoaderInitialized) {
            return librariesClassLoader;
        }
        Set<Library> libs = mainRepo.getLibraries();
        HashSet libUrls = new HashSet();
        if (libs != null && libs.size() > 0) {
            log.debug(Markers.SYSTEM, "Loading libraries...");
            int numNPM = 0;
            for (Library lib : libs) {
                if (lib.type.equals("java")) {
                    URL[] urls = IvyRepository.getInstance().getLibraryWithDependencies(lib.orgId, lib.artifactId, lib.version);
                    Collections.addAll(libUrls, urls);
                    continue;
                }
                if (!lib.type.equals("javascript") || NPMManager.moduleIsInstalled(lib.artifactId, lib.version)) continue;
                NPMManager.installLibrary(lib.artifactId, lib.version);
                ++numNPM;
            }
            if (numNPM > 0) {
                log.info(Markers.SYSTEM, "JavaScript libraries installed: " + numNPM);
            }
            if (libUrls.size() > 0) {
                URL[] allUrls = new URL[libUrls.size()];
                int i = 0;
                Iterator iterator = libUrls.iterator();
                while (iterator.hasNext()) {
                    URL url;
                    allUrls[i] = url = (URL)iterator.next();
                    ++i;
                }
                librariesClassLoader = new URLClassLoader(allUrls, RepositoryManager.class.getClassLoader());
                log.info(Markers.SYSTEM, "Java libraries loaded: " + libUrls.size());
            } else {
                librariesClassLoader = null;
            }
        } else {
            librariesClassLoader = null;
        }
        librariesClassLoaderInitialized = true;
        return librariesClassLoader;
    }

    public static void resetLibrariesClassLoader() {
        librariesClassLoaderInitialized = false;
        RepositoryManager.getLibrariesClassLoader();
    }

    private static Path createBackupDirectory() {
        String backupLoc = SettingsManager.getInstance().getStringSetting(SettingName.BACKUP_LOCATION);
        if (backupLoc == null || backupLoc.trim().length() == 0) {
            return null;
        }
        Path backupPath = Paths.get(backupLoc, new String[0]);
        if (!Files.exists(backupPath, new LinkOption[0])) {
            throw new RepositoryException("repo.backup.NoSuchDirectory", backupLoc);
        }
        if (!Files.isDirectory(backupPath, new LinkOption[0])) {
            throw new RepositoryException("repo.backup.NotADirectory", backupLoc);
        }
        if (!Files.isWritable(backupPath)) {
            throw new RepositoryException("repo.backup.NotWriteable", backupLoc);
        }
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        String separator = FileSystems.getDefault().getSeparator();
        String dateStr = String.format("%d" + separator + "%02d" + separator + "%02d" + separator + "%02d%02d%02d", cal.get(1), cal.get(2) + 1, cal.get(5), cal.get(11), cal.get(12), cal.get(13));
        Path datePath = backupPath.resolve(dateStr);
        if (!Files.exists(datePath, new LinkOption[0])) {
            try {
                Files.createDirectories(datePath, new FileAttribute[0]);
            }
            catch (IOException ioex) {
                throw new RepositoryException("repo.backup.CannotCreateDirectory", datePath, ioex.getMessage());
            }
        }
        return datePath;
    }

    private static void copyRepositoryToBackup(final Path target) {
        log.debug(Markers.REPO, "Copying repository to backup: {}", (Object)target);
        final Path source = RepositoryManager.getMainRepository().path.getParent();
        try {
            Files.walkFileTree(source, (FileVisitor<? super Path>)new SimpleFileVisitor<Path>(){

                @Override
                public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs) throws IOException {
                    Path newPath = target.resolve(source.relativize(path));
                    Files.createDirectories(newPath, new FileAttribute[0]);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.copy(file, target.resolve(source.relativize(file)), new CopyOption[0]);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch (IOException ioex) {
            throw new RepositoryException("repo.backup.ErrorCreatingBackup", ioex.getMessage());
        }
    }

    private static void deleteRepository() {
        log.debug(Markers.REPO, "Deleting repository");
        final Path source = RepositoryManager.getMainRepository().path.getParent();
        try {
            Files.walkFileTree(source, (FileVisitor<? super Path>)new SimpleFileVisitor<Path>(){

                @Override
                public FileVisitResult postVisitDirectory(Path path, IOException ex) throws IOException {
                    if (!path.equals(source)) {
                        Files.delete(path);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                    Files.delete(path);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch (AccessDeniedException adex) {
            throw new RepositoryException("repo.backup.ErrorDeletingRepository", "Insufficient permissions - " + adex.getMessage());
        }
        catch (IOException ioex) {
            throw new RepositoryException("repo.backup.ErrorDeletingRepository", ioex.getMessage());
        }
    }

    private static void unzipRepository(ZipInputStream zis) throws IOException {
        log.debug(Markers.REPO, "Expanding new repository");
        ZipEntry zipEntry = zis.getNextEntry();
        Path rootPath = RepositoryManager.getMainRepository().path.getParent();
        byte[] buffer = new byte[1024];
        while (zipEntry != null) {
            int len;
            File newFile = RepositoryManager.newFile(rootPath.toFile(), zipEntry);
            if (newFile == null) {
                zipEntry = zis.getNextEntry();
                continue;
            }
            if (zipEntry.isDirectory()) {
                Files.createDirectories(newFile.toPath(), new FileAttribute[0]);
                zipEntry = zis.getNextEntry();
                continue;
            }
            FileOutputStream fos = new FileOutputStream(newFile);
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    }

    private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        String name = zipEntry.getName();
        String[] nameParts = name.split("/");
        if (nameParts.length == 1) {
            return null;
        }
        Path filePath = Paths.get(zipEntry.getName(), new String[0]);
        filePath = filePath.subpath(1, filePath.getNameCount());
        File destFile = new File(destinationDir, filePath.toString());
        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();
        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new RepositoryException("repo.backup.InvalidRepositoryArchive", zipEntry.getName());
        }
        return destFile;
    }

    static {
        log = LogManager.getLogger((String)"galliumdata.core");
    }
}
