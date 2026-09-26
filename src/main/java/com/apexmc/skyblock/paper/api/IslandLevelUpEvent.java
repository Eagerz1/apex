package com.apexmc.skyblock.paper.api;

import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired after an island gained one or more progression levels. */
public class IslandLevelUpEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID islandId;
    private final UUID playerId;
    private final int previousLevel;
    private final int newLevel;

    public IslandLevelUpEvent(UUID islandId, UUID playerId, int previousLevel, int newLevel) {
        this.islandId = islandId;
        this.playerId = playerId;
        this.previousLevel = previousLevel;
        this.newLevel = newLevel;
    }

    public UUID islandId() {
        return islandId;
    }

    public UUID playerId() {
        return playerId;
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
