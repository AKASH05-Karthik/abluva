/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.JsonNode
 */
package com.galliumdata.server.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.galliumdata.server.repository.RepositoryException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

public class RepositoryUtil {
    public static void checkFile(Path path) {
        if (!Files.exists(path, new LinkOption[0])) {
            throw new RepositoryException("repo.BadLocation", path.toAbsolutePath().toString(), "Does not exist");
        }
        if (Files.isDirectory(path, new LinkOption[0])) {
            throw new RepositoryException("repo.BadLocation", path.toAbsolutePath().toString(), "Is a directory");
        }
        if (!Files.isRegularFile(path, new LinkOption[0])) {
            throw new RepositoryException("repo.BadLocation", path.toAbsolutePath().toString(), "Is not a file");
        }
        if (!Files.isReadable(path)) {
            throw new RepositoryException("repo.BadLocation", path.toAbsolutePath().toString(), "Not readable");
        }
    }

    public static void checkDirectory(Path path) {
        if (!Files.exists(path, new LinkOption[0])) {
            throw new RepositoryException("repo.BadLocation", path.toAbsolutePath().toString(), "Does not exist");
        }
        if (!Files.isDirectory(path, new LinkOption[0])) {
            throw new RepositoryException("repo.BadLocation", path.toAbsolutePath().toString(), "Is a directory");
        }
        if (!Files.isReadable(path)) {
            throw new RepositoryException("repo.BadLocation", path.toAbsolutePath().toString(), "Not readable");
        }
    }

    public static Object getJsonNodeValue(JsonNode node, String context) {
        if (null == node) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        if (node.isFloatingPointNumber()) {
            return node.asDouble();
        }
        if (node.isIntegralNumber()) {
            return node.asInt();
        }
        if (node.isNull()) {
            return null;
        }
        throw new RepositoryException("repo.BadProperty", context, node.getNodeType());
    }
}
