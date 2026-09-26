package org.bukkit.configuration;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ConfigurationSection {
    Set<String> getKeys(boolean deep);
    Object get(String path);
    ConfigurationSection getConfigurationSection(String path);
    boolean isConfigurationSection(String path);
    String getString(String path);
    int getInt(String path);
    boolean getBoolean(String path);
    double getDouble(String path);
    List<?> getList(String path);
    Map<String, Object> getValues(boolean deep);
}
