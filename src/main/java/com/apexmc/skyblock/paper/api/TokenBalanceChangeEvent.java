package com.apexmc.skyblock.paper.api;

import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired after a Sky Token balance changed. */
public class TokenBalanceChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerId;
    private final long delta;
    private final long newBalance;
    private final String reason;

    public TokenBalanceChangeEvent(UUID playerId, long delta, long newBalance, String reason) {
        this.playerId = playerId;
        this.delta = delta;
        this.newBalance = newBalance;
        this.reason = reason;
    }

    public UUID playerId() {
        return playerId;
    }

    public long delta() {
        return delta;
    }

    public long newBalance() {
        return newBalance;
    }

    public String reason() {
        return reason;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
