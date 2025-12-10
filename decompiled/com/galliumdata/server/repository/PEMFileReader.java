/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package com.galliumdata.server.repository;

import com.galliumdata.server.log.Markers;
import com.galliumdata.server.repository.RepositoryException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PEMFileReader {
    private static final String LABEL_BEGIN = "-----BEGIN ";
    private static final String LABEL_END = "-----";
    private static final String END_BEGIN = "-----END ";
    private static final Logger log = LogManager.getLogger((String)"galliumdata.core");

    public static List<PEMEntry> readPEMFile(String filename) {
        ArrayList<PEMEntry> entries = new ArrayList<PEMEntry>();
        try {
            byte[] pemBytes = Files.readAllBytes(Paths.get(filename, new String[0]));
            String pem = new String(pemBytes);
            int idx = 0;
            PEMEntry entry = PEMFileReader.readPEMSection(pem, idx, filename);
            while (entry != null) {
                entries.add(entry);
                entry = PEMFileReader.readPEMSection(pem, idx += entry.bytesRead, filename);
            }
        }
        catch (IllegalArgumentException iae) {
            throw new RepositoryException("repo.crypto.ErrorReadingPEM", filename, "most likely the certificate text contains invalid content");
        }
        catch (Exception ex) {
            throw new RepositoryException("repo.crypto.ErrorReadingPEM", filename, ex.getMessage());
        }
        return entries;
    }

    private static PEMEntry readPEMSection(String s, int idx, String filename) {
        int lenIdx;
        String bytesStr;
        int beginIdx = s.indexOf(LABEL_BEGIN, idx);
        if (beginIdx == -1) {
            return null;
        }
        int labelEndIdx = s.indexOf(LABEL_END, beginIdx + LABEL_BEGIN.length());
        if (labelEndIdx == -1) {
            throw new RuntimeException("Unable to find label end, i.e. ----- after -----BEGIN");
        }
        PEMEntry entry = new PEMEntry();
        entry.label = s.substring(beginIdx + LABEL_BEGIN.length(), labelEndIdx).trim();
        int endIdx = s.indexOf(END_BEGIN, labelEndIdx + LABEL_END.length());
        if (endIdx == -1) {
            throw new RuntimeException("Unable to find section end, i.e. -----END ");
        }
        int endEndIdx = s.indexOf(LABEL_END, endIdx + END_BEGIN.length());
        if (endEndIdx == -1) {
            throw new RuntimeException("Unable to find label end, i.e. ----- after -----END");
        }
        String endLabel = s.substring(endIdx + END_BEGIN.length(), endEndIdx);
        if (!entry.label.equals(endLabel)) {
            log.warn(Markers.REPO, "PEM entry has mismatched BEGIN and END label: " + entry.label + " vs " + endLabel + " in file " + filename + ". This is only advisory, the entry will be read anyway.");
        }
        if ((bytesStr = s.substring(labelEndIdx + LABEL_END.length(), endIdx)).charAt(0) == '\n' || bytesStr.charAt(0) == '\r') {
            bytesStr = bytesStr.substring(1);
        }
        if (bytesStr.charAt(lenIdx = bytesStr.length() - 1) == '\n' || bytesStr.charAt(lenIdx) == '\r') {
            bytesStr = bytesStr.substring(0, lenIdx);
        }
        bytesStr = bytesStr.replaceAll("([^\\r])\\n", "$1\r\n");
        bytesStr = bytesStr.replaceAll("\\r([^\\n])", "\r\n$1");
        entry.bytes = Base64.getMimeDecoder().decode(bytesStr);
        entry.bytesRead = endEndIdx + LABEL_END.length() - idx;
        return entry;
    }

    public static class PEMEntry {
        public String label;
        public byte[] bytes;
        public int bytesRead;

        public int hashCode() {
            return Arrays.hashCode(this.bytes);
        }
    }
}
