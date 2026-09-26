package org.bukkit.event.inventory;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;

public class InventoryCloseEvent extends Event {
    public Inventory getInventory() { return null; }
    public org.bukkit.entity.HumanEntity getPlayer() { return null; }
    @Override public HandlerList getHandlers() { return null; }
}
