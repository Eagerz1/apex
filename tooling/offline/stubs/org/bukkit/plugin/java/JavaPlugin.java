package org.bukkit.plugin.java;

import java.io.File;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

public abstract class JavaPlugin implements Plugin {
    public void onEnable() { }
    public void onDisable() { }
    public void onLoad() { }
    @Override public String getName() { return "plugin"; }
    @Override public File getDataFolder() { return new File("."); }
    @Override public FileConfiguration getConfig() { return null; }
    @Override public void saveDefaultConfig() { }
    @Override public void reloadConfig() { }
    @Override public Logger getLogger() { return Logger.getLogger("plugin"); }
    @Override public Server getServer() { return null; }
    @Override public boolean isEnabled() { return true; }
    public PluginCommand getCommand(String name) { return null; }
    public void saveResource(String resourcePath, boolean replace) { }
}
