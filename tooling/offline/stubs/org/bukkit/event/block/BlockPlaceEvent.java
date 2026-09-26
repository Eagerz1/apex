package org.bukkit.event.block;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public class BlockPlaceEvent extends Event implements Cancellable {
    public Block getBlockPlaced() { return null; }
    public Player getPlayer() { return null; }
    public ItemStack getItemInHand() { return null; }
    @Override public boolean isCancelled() { return false; }
    @Override public void setCancelled(boolean cancel) { }
    @Override public HandlerList getHandlers() { return null; }
}
