package org.bukkit.configuration.file;

import java.io.File;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;

public class YamlConfiguration extends FileConfiguration {
    public static YamlConfiguration loadConfiguration(File file) { return new YamlConfiguration(); }
    public static YamlConfiguration loadConfiguration(Reader reader) { return new YamlConfiguration(); }
    @Override public Set<String> getKeys(boolean deep) { return Set.of(); }
    @Override public Object get(String path) { return null; }
    @Override public ConfigurationSection getConfigurationSection(String path) { return null; }
    @Override public boolean isConfigurationSection(String path) { return false; }
    @Override public String getString(String path) { return null; }
    @Override public int getInt(String path) { return 0; }
    @Override public boolean getBoolean(String path) { return false; }
    @Override public double getDouble(String path) { return 0; }
    @Override public List<?> getList(String path) { return List.of(); }
    @Override public Map<String, Object> getValues(boolean deep) { return Map.of(); }
}
