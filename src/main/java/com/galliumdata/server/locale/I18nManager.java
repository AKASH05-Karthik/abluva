package com.galliumdata.server.locale;

public class I18nManager {
    public static String getString(String key, Object... args) {
        // Simple implementation - just return the key and args
        StringBuilder result = new StringBuilder(key);
        if (args != null && args.length > 0) {
            result.append(": ");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) result.append(", ");
                result.append(args[i]);
            }
        }
        return result.toString();
    }
}
