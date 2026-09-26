package com.apexmc.skyblock.paper.api;

import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired after a player's role gained one or more levels. */
public class RoleLevelUpEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerId;
    private final String roleId;
    private final int previousLevel;
    private final int newLevel;

    public RoleLevelUpEvent(UUID playerId, String roleId, int previousLevel, int newLevel) {
        this.playerId = playerId;
        this.roleId = roleId;
        this.previousLevel = previousLevel;
        this.newLevel = newLevel;
    }

    public UUID playerId() {
        return playerId;
    }

    public String roleId() {
        return roleId;
    }

    public int previousLevel() {
        return previousLevel;
    }

    public int newLevel() {
        return newLevel;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
