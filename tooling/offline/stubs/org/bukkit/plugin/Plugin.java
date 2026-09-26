package org.bukkit.plugin;

import java.io.File;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;

public interface Plugin {
    String getName();
    File getDataFolder();
    FileConfiguration getConfig();
    void saveDefaultConfig();
    void reloadConfig();
    Logger getLogger();
    Server getServer();
    boolean isEnabled();
}
