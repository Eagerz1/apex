package org.bukkit.event.player;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerQuitEvent extends Event {
    public Player getPlayer() { return null; }
    @Override public HandlerList getHandlers() { return null; }
}
