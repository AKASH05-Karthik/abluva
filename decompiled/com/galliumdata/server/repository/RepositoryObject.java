/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.JsonNode
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package com.galliumdata.server.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.galliumdata.server.Zippable;
import com.galliumdata.server.log.Markers;
import com.galliumdata.server.repository.NullClass;
import com.galliumdata.server.repository.Persisted;
import com.galliumdata.server.repository.Repository;
import com.galliumdata.server.repository.RepositoryException;
import com.galliumdata.server.repository.RepositoryUtil;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class RepositoryObject
implements Zippable {
    protected Repository repository;
    protected RepositoryObject parentObject;
    protected Path path;
    protected boolean isRead = false;
    @Persisted(JSONName="active")
    protected boolean isActive = true;
    protected String name;
    private static final Logger log = LogManager.getLogger((String)"galliumdata.core");

    public RepositoryObject() {
    }

    public RepositoryObject(Repository repo) {
        this.repository = repo;
    }

    protected void setRepository(Repository repo) {
        this.repository = repo;
    }

    public RepositoryObject getParentObject() {
        return this.parentObject;
    }

    protected void setParentObject(RepositoryObject parent) {
        this.parentObject = parent;
    }

    public void readFromJson() {
        JsonNode node;
        if (this.isRead) {
            return;
        }
        this.isRead = true;
        ObjectMapper mapper = new ObjectMapper();
        try (BufferedReader reader = Files.newBufferedReader(this.path);){
            node = mapper.readTree((Reader)reader);
        }
        catch (Exception ex) {
            throw new RepositoryException("repo.BadFile", this.path.toString(), ex.getMessage());
        }
        Field[] fields = this.getClass().getDeclaredFields();
        Field[] superFields = this.getClass().getSuperclass().getDeclaredFields();
        Field[] allFields = new Field[fields.length + superFields.length];
        System.arraycopy(fields, 0, allFields, 0, fields.length);
        System.arraycopy(superFields, 0, allFields, fields.length, superFields.length);
        for (int i = 0; i < allFields.length; ++i) {
            String subDirName;
            Field field = allFields[i];
            Persisted pers = field.getAnnotation(Persisted.class);
            if (null == pers || !"".equals(subDirName = pers.directoryName())) continue;
            String jsonName = pers.JSONName();
            Class<?> cls = field.getType();
            Serializable value = null;
            JsonNode valueNode = node.get(jsonName);
            if (valueNode == null) {
                value = null;
            } else if (cls.equals(String.class)) {
                value = valueNode.asText();
                if (pers.allowedValues().length > 0 && !Arrays.asList(pers.allowedValues()).contains(value)) {
                    throw new RepositoryException("repo.InvalidPropertyValue", jsonName, value, this.getName());
                }
            } else if (cls.equals(Boolean.class) || cls.equals(Boolean.TYPE)) {
                value = valueNode.booleanValue();
            } else if (cls.equals(Integer.TYPE) || cls.equals(Integer.class)) {
                value = valueNode.isNull() ? Integer.valueOf(0) : Integer.valueOf(valueNode.intValue());
            } else if (cls.equals(Map.class)) {
                value = new HashMap();
                Iterator iter = valueNode.fields();
                while (iter.hasNext()) {
                    Map.Entry entry = (Map.Entry)iter.next();
                    String name = (String)entry.getKey();
                    Object val = RepositoryUtil.getJsonNodeValue((JsonNode)entry.getValue(), "Property " + name + " in " + String.valueOf(this.path));
                    ((Map)((Object)value)).put(name, val);
                }
            } else if (cls.equals(Set.class)) {
                value = new HashSet();
                for (int j = 0; j < valueNode.size(); ++j) {
                    JsonNode libNode = valueNode.get(j);
                    Constructor<? extends RepositoryObject> constructor = null;
                    try {
                        constructor = pers.memberClass().getConstructor(Repository.class, JsonNode.class);
                        RepositoryObject newObj = constructor.newInstance(this.repository, libNode);
                        ((Set)((Object)value)).add(newObj);
                        continue;
                    }
                    catch (InvocationTargetException itex) {
                        throw new RepositoryException("repo.ErrorLoading", itex.getTargetException().getMessage());
                    }
                    catch (Exception ex) {
                        throw new RepositoryException("repo.ErrorLoading", ex.getMessage());
                    }
                }
            } else {
                throw new RepositoryException("", field.toString(), cls.getName());
            }
            if (null == value) continue;
            try {
                field.set(this, value);
                continue;
            }
            catch (Exception ex) {
                throw new RepositoryException("repo.AccessError", field.getName(), "repository", ex.getMessage());
            }
        }
    }

    public void forgetEverything() {
    }

    @Override
    public byte[] zip() throws IOException {
        ByteArrayOutputStream fos = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        File fileToZip = this.path.getParent().toFile();
        this.zipFile(fileToZip, fileToZip.getName(), zipOut);
        zipOut.close();
        fos.close();
        return fos.toByteArray();
    }

    protected void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        int length;
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            File[] children;
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                zipOut.closeEntry();
            }
            for (File childFile : children = fileToZip.listFiles()) {
                this.zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
            }
            return;
        }
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        fis.close();
    }

    protected void readCollectionFromJSON(Field fld) {
        Persisted pers = fld.getAnnotation(Persisted.class);
        if (null == pers) {
            return;
        }
        String subDirName = pers.directoryName();
        Path subDirPath = this.path.resolveSibling(subDirName);
        if (!Files.exists(subDirPath, new LinkOption[0])) {
            return;
        }
        if (!Files.isDirectory(subDirPath, new LinkOption[0])) {
            throw new RepositoryException("repo.BadLocation", subDirPath, "not a directory");
        }
        if (!Files.isReadable(subDirPath)) {
            throw new RepositoryException("repo.BadLocation", subDirPath, "cannot read");
        }
        Class<? extends RepositoryObject> compCls = pers.memberClass();
        if (compCls.equals(NullClass.class)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(subDirPath, 1, new FileVisitOption[0]);){
            paths.forEach(t -> this.addFileToField(fld, pers, (Path)t));
        }
        catch (IOException ioex) {
            throw new RuntimeException(ioex);
        }
    }

    private void addFileToField(Field field, Persisted pers, Path path) {
        block16: {
            Class<? extends RepositoryObject> compCls;
            Path fileToReadPath = path;
            String subFileName = pers.fileName();
            if (Files.isDirectory(path, new LinkOption[0])) {
                if ("".equals(subFileName)) {
                    return;
                }
                if (!Files.exists(fileToReadPath = fileToReadPath.resolve(subFileName), new LinkOption[0])) {
                    return;
                }
            } else if (!path.getFileName().toString().endsWith(".json")) {
                return;
            }
            if ((compCls = pers.memberClass()).equals(NullClass.class)) {
                throw new RepositoryException("repo.UnexpectedDirectory", path);
            }
            Class<?> fldCls = field.getType();
            Class<?> collCls = null;
            Class<?> mapCls = null;
            if (Map.class.isAssignableFrom(fldCls)) {
                mapCls = fldCls;
            } else if (Collection.class.isAssignableFrom(fldCls)) {
                collCls = fldCls;
            }
            try {
                String objName;
                Constructor<? extends RepositoryObject> constructor = compCls.getConstructor(Repository.class);
                RepositoryObject newChild = constructor.newInstance(this.repository);
                newChild.path = fileToReadPath;
                newChild.setParentObject(this);
                newChild.readFromJson();
                Object fieldValueObj = field.get(this);
                if (collCls != null) {
                    Method method = collCls.getMethod("add", Object.class);
                    method.invoke(fieldValueObj, newChild);
                    break block16;
                }
                Method method = mapCls.getMethod("put", Object.class, Object.class);
                if ("".equals(subFileName)) {
                    objName = fileToReadPath.getName(fileToReadPath.getNameCount() - 1).toString();
                    objName = objName.substring(0, objName.length() - 5);
                } else {
                    objName = fileToReadPath.getName(fileToReadPath.getNameCount() - 2).toString();
                }
                try {
                    method.invoke(fieldValueObj, objName, newChild);
                }
                catch (Exception ex2) {
                    log.debug(Markers.REPO, "Exception calling method " + method.getName() + " : " + ex2.getMessage());
                    throw ex2;
                }
                newChild.setName(objName);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public String getComments() {
        Path commentsPath = this.path.resolveSibling("comments.md");
        if (!Files.exists(commentsPath, new LinkOption[0])) {
            return null;
        }
        if (!Files.isRegularFile(commentsPath, new LinkOption[0])) {
            throw new RepositoryException("repo.BadLocation", commentsPath, "not a regular file");
        }
        if (!Files.isReadable(commentsPath)) {
            throw new RepositoryException("repo.BadLocation", commentsPath, "cannot read");
        }
        try {
            return Files.readString(commentsPath);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String s) {
        this.name = s;
    }

    public boolean isActive() {
        return this.isActive;
    }

    public Path getPath() {
        return this.path;
    }

    public String getCode() {
        Path codePath = this.path.resolveSibling("code.js");
        if (!Files.exists(codePath, new LinkOption[0])) {
            return null;
        }
        if (!Files.isRegularFile(codePath, new LinkOption[0])) {
            throw new RepositoryException("repo.BadLocation", codePath, "not a regular file");
        }
        if (!Files.isReadable(codePath)) {
            throw new RepositoryException("repo.BadLocation", codePath, "cannot read");
        }
        try {
            return Files.readString(codePath);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
