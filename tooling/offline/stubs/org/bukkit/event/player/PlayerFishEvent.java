package org.bukkit.event.player;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerFishEvent extends Event implements Cancellable {
    public enum State { FISHING, CAUGHT_FISH, CAUGHT_ENTITY, IN_GROUND, FAILED_ATTEMPT, REEL_IN, BITE }
    public Player getPlayer() { return null; }
    public State getState() { return State.FISHING; }
    public Entity getCaught() { return null; }
    @Override public boolean isCancelled() { return false; }
    @Override public void setCancelled(boolean cancel) { }
    @Override public HandlerList getHandlers() { return null; }
}
