/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.util;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class StringUtil {
    public static boolean stringIsInteger(String s) {
        if (s == null) {
            return false;
        }
        return s.chars().allMatch(Character::isDigit);
    }

    public static String getFirstWord(String s) {
        s = s.trim();
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (!Character.isWhitespace(c)) continue;
            return s.substring(0, i);
        }
        return s;
    }

    public static String getLastWord(String s) {
        s = s.trim();
        for (int i = s.length() - 1; i > 0; --i) {
            char c = s.charAt(i);
            if (!Character.isWhitespace(c)) continue;
            return s.substring(i + 1);
        }
        return s;
    }

    public static byte[] getUTF8BytesForString(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    public static String stringFromUTF8Bytes(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static String padString(String s, int n) {
        if (s == null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < n; ++i) {
                sb.append(' ');
            }
            return sb.toString();
        }
        if (((String)s).length() > n) {
            s = ((String)s).substring(0, n);
        }
        for (int i = ((String)s).length(); i < n; ++i) {
            s = (String)s + " ";
        }
        return s;
    }

    public static String getZeroPaddedNumber(int n, int numDigits) {
        Object sn;
        if (n < 0) {
            n = -n;
        }
        if (((String)(sn = "" + n)).length() > numDigits) {
            sn = ((String)sn).substring(0, numDigits);
        }
        if (((String)sn).length() < numDigits) {
            sn = "0".repeat(numDigits - ((String)sn).length()) + (String)sn;
        }
        return sn;
    }

    public static String getShortenedString(String s, int len) {
        if (s == null) {
            return "null";
        }
        if (len == 0 || s.length() <= len) {
            return s;
        }
        int l = s.length();
        return s.substring(0, len) + "[" + (l - len) + " more]";
    }

    public static String getCommaSeparatedStringList(List<? extends Object> values, int maxValueLen) {
        StringBuilder sb = new StringBuilder();
        for (Object object : values) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            if (object == null) {
                sb.append("null");
                continue;
            }
            String s = object.toString();
            s = StringUtil.getShortenedString(s, maxValueLen);
            sb.append(s);
        }
        return sb.toString();
    }
}
