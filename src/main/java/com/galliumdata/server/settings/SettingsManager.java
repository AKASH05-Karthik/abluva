package com.galliumdata.server.settings;

import java.util.HashMap;
import java.util.Map;

public class SettingsManager {
    private static SettingsManager instance;
    private Map<SettingName, String> settings = new HashMap<>();
    
    public static void initialize(String[] args) {
        instance = new SettingsManager();
    }
    
    public static SettingsManager getInstance() {
        return instance;
    }
    
    public String getStringSetting(SettingName name) {
        return settings.get(name);
    }
    
    public void setSetting(SettingName name, String value) {
        settings.put(name, value);
    }
}
