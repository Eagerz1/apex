package org.bukkit.event.inventory;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

public class InventoryClickEvent extends Event implements Cancellable {
    public Inventory getClickedInventory() { return null; }
    public Inventory getInventory() { return null; }
    public InventoryView getView() { return null; }
    public int getSlot() { return 0; }
    public int getRawSlot() { return 0; }
    public ItemStack getCurrentItem() { return null; }
    public ClickType getClick() { return ClickType.LEFT; }
    public InventoryAction getAction() { return InventoryAction.NOTHING; }
    public org.bukkit.entity.HumanEntity getWhoClicked() { return null; }
    @Override public boolean isCancelled() { return false; }
    @Override public void setCancelled(boolean cancel) { }
    @Override public HandlerList getHandlers() { return null; }
}
