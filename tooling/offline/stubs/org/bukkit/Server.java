package org.bukkit;

import java.util.Collection;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.scheduler.BukkitScheduler;

public interface Server {
    PluginManager getPluginManager();
    BukkitScheduler getScheduler();
    ServicesManager getServicesManager();
    ConsoleCommandSender getConsoleSender();
    Collection<? extends Player> getOnlinePlayers();
    Player getPlayer(String name);
    World getWorld(String name);
    Inventory createInventory(InventoryHolder owner, int size, String title);
    boolean dispatchCommand(org.bukkit.command.CommandSender sender, String command);
    void broadcastMessage(String message);
}
