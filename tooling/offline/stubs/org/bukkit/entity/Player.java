package org.bukkit.entity;

import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.PlayerInventory;

public interface Player extends LivingEntity, CommandSender, OfflinePlayer {
    @Override UUID getUniqueId();
    @Override String getName();
    PlayerInventory getInventory();
    InventoryView openInventory(Inventory inventory);
    void closeInventory();
    void playSound(Location location, Sound sound, float volume, float pitch);
    @Override Location getLocation();
    void updateInventory();
}
