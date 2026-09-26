package com.apexmc.skyblock.core.data;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** All persisted per-island progression (island XP, mastery, manual unlocks). */
public final class IslandProfile {

    private final UUID islandId;
    private UUID ownerId;
    private String seasonId;
    private long islandXp;
    private final Map<String, Long> masteryXp = new LinkedHashMap<>();
    private final Set<String> grantedUnlocks = new LinkedHashSet<>();
    private volatile boolean dirty;

    public IslandProfile(UUID islandId, UUID ownerId, String seasonId) {
        this.islandId = islandId;
        this.ownerId = ownerId;
        this.seasonId = seasonId;
    }

    public UUID islandId() {
        return islandId;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public void ownerId(UUID ownerId) {
        this.ownerId = ownerId;
        markDirty();
    }

    public String seasonId() {
        return seasonId;
    }

    public void seasonId(String seasonId) {
        this.seasonId = seasonId;
        markDirty();
    }

    public long islandXp() {
        return islandXp;
    }

    public void islandXp(long islandXp) {
        this.islandXp = Math.max(0L, islandXp);
        markDirty();
    }

    public long masteryXp(String categoryId) {
        return masteryXp.getOrDefault(categoryId, 0L);
    }

    public void masteryXp(String categoryId, long xp) {
        masteryXp.put(categoryId, Math.max(0L, xp));
        markDirty();
    }

    public Map<String, Long> masteryXpView() {
        return java.util.Collections.unmodifiableMap(masteryXp);
    }

    /** Unlocks granted outside the island-level table (admin grants, purchases). */
    public Set<String> grantedUnlocks() {
        return grantedUnlocks;
    }

    public boolean dirty() {
        return dirty;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public void clearDirty() {
        this.dirty = false;
    }
}
