package com.apexmc.skyblock.core.data;

import java.util.UUID;

/** A companion owned by a player. Level is always derived from XP by the service. */
public final class CompanionInstance {

    private final UUID instanceId;
    private final String definitionId;
    private String rarityId;
    private long xp;

    public CompanionInstance(UUID instanceId, String definitionId, String rarityId, long xp) {
        this.instanceId = instanceId;
        this.definitionId = definitionId;
        this.rarityId = rarityId;
        this.xp = Math.max(0L, xp);
    }

    public UUID instanceId() {
        return instanceId;
    }

    public String definitionId() {
        return definitionId;
    }

    public String rarityId() {
        return rarityId;
    }

    public void rarityId(String rarityId) {
        this.rarityId = rarityId;
    }

    public long xp() {
        return xp;
    }

    public void xp(long xp) {
        this.xp = Math.max(0L, xp);
    }
}
