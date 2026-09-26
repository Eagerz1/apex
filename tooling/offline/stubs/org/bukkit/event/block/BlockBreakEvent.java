package org.bukkit.event.block;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BlockBreakEvent extends Event implements Cancellable {
    public Block getBlock() { return null; }
    public Player getPlayer() { return null; }
    public boolean isDropItems() { return true; }
    public void setDropItems(boolean drop) { }
    @Override public boolean isCancelled() { return false; }
    @Override public void setCancelled(boolean cancel) { }
    @Override public HandlerList getHandlers() { return null; }
}
