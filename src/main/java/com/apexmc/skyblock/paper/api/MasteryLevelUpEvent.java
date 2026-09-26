package com.apexmc.skyblock.paper.api;

import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired after an island mastery category gained one or more levels. */
public class MasteryLevelUpEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID islandId;
    private final String categoryId;
    private final int previousLevel;
    private final int newLevel;

    public MasteryLevelUpEvent(UUID islandId, String categoryId, int previousLevel, int newLevel) {
        this.islandId = islandId;
        this.categoryId = categoryId;
        this.previousLevel = previousLevel;
        this.newLevel = newLevel;
    }

    public UUID islandId() {
        return islandId;
    }

    public String categoryId() {
        return categoryId;
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
