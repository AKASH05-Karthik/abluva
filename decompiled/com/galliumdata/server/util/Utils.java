/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.io.IOUtils
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package com.galliumdata.server.util;

import com.galliumdata.server.adapters.Variables;
import com.galliumdata.server.util.BinaryDump;
import com.galliumdata.server.util.GeneralCache;
import com.galliumdata.server.util.StringUtil;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Utils {
    private static final Logger log = LogManager.getLogger((String)"galliumdata.uselog");

    public byte[] getUTF8BytesForString(String s) {
        return StringUtil.getUTF8BytesForString(s);
    }

    public String stringFromUTF8Bytes(byte[] bytes) {
        return StringUtil.stringFromUTF8Bytes(bytes);
    }

    public String stringFromUTF8Bytes(byte[] bytes, int offset, int numBytes) {
        return new String(bytes, offset, numBytes, StandardCharsets.UTF_8);
    }

    public Variables createObject() {
        return new Variables();
    }

    public byte[] allocateByteArray(int size) {
        return new byte[size];
    }

    public String getBinaryDump(byte[] bytes) {
        return BinaryDump.getBinaryDump(bytes, 0, bytes.length);
    }

    public String getBinaryDump(byte[] bytes, int offset, int length) {
        return BinaryDump.getBinaryDump(bytes, offset, length);
    }

    public GeneralCache createCache(int maxSize, long maxIdleTimeInSecs) {
        return new GeneralCache(maxSize, maxIdleTimeInSecs);
    }

    public String doPost(String restUrl, String payload) {
        log.trace("doPost called for URL: {}", (Object)restUrl);
        try {
            URL url = new URL(restUrl);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            OutputStream os = conn.getOutputStream();
            byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
            os.write(bytes, 0, bytes.length);
            int respStatus = conn.getResponseCode();
            if (respStatus == 200) {
                log.trace("doPost successful");
                return IOUtils.toString((InputStream)conn.getInputStream(), (Charset)StandardCharsets.UTF_8);
            }
            log.debug("doPost failed: {}", (Object)respStatus);
            return null;
        }
        catch (Exception ex) {
            log.debug("doPost failed with exception", (Throwable)ex);
            return null;
        }
    }
}
