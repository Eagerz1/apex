package org.bukkit.event.inventory;

import java.util.Map;
import java.util.Set;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

public class InventoryDragEvent extends Event implements Cancellable {
    public Inventory getInventory() { return null; }
    public InventoryView getView() { return null; }
    public Set<Integer> getRawSlots() { return Set.of(); }
    public Map<Integer, ItemStack> getNewItems() { return Map.of(); }
    public org.bukkit.entity.HumanEntity getWhoClicked() { return null; }
    @Override public boolean isCancelled() { return false; }
    @Override public void setCancelled(boolean cancel) { }
    @Override public HandlerList getHandlers() { return null; }
}
