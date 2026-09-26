package org.bukkit;

import java.util.Collection;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.scheduler.BukkitScheduler;

public final class Bukkit {
    public static Server getServer() { return null; }
    public static PluginManager getPluginManager() { return null; }
    public static BukkitScheduler getScheduler() { return null; }
    public static ServicesManager getServicesManager() { return null; }
    public static Collection<? extends Player> getOnlinePlayers() { return java.util.List.of(); }
    public static Player getPlayer(UUID id) { return null; }
    public static Player getPlayer(String name) { return null; }
    public static OfflinePlayer getOfflinePlayer(UUID id) { return null; }
    public static OfflinePlayer getOfflinePlayer(String name) { return null; }
    public static World getWorld(String name) { return null; }
    public static Inventory createInventory(InventoryHolder owner, int size, String title) { return null; }
    public static org.bukkit.command.ConsoleCommandSender getConsoleSender() { return null; }
    public static boolean dispatchCommand(org.bukkit.command.CommandSender sender, String command) { return true; }
    public static boolean isPrimaryThread() { return true; }
}
