package org.bukkit;

import java.util.Collection;
import java.util.UUID;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

public interface World {
    String getName();
    UUID getUID();
    Block getBlockAt(int x, int y, int z);
    Entity spawnEntity(Location location, EntityType type);
    void dropItemNaturally(Location location, ItemStack item);
    Collection<Entity> getNearbyEntities(Location location, double x, double y, double z);
    void playSound(Location location, Sound sound, float volume, float pitch);
}
