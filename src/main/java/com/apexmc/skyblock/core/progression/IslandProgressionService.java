package com.apexmc.skyblock.core.progression;

import com.apexmc.skyblock.core.data.IslandProfile;
import com.apexmc.skyblock.core.util.Ids;
import java.util.ArrayList;
import java.util.List;

/**
 * Island level progression. XP comes from active gameplay and every level can
 * unlock mechanics rather than just larger numbers.
 */
public final class IslandProgressionService {

    private IslandLevelConfig config;
    private double globalMultiplier = 1.0d;

    public IslandProgressionService(IslandLevelConfig config) {
        this.config = config;
    }

    public void reload(IslandLevelConfig config) {
        this.config = config;
    }

    public IslandLevelConfig config() {
        return config;
    }

    /** Seasonal (or event) multiplier applied to every island XP gain. */
    public void globalMultiplier(double multiplier) {
        this.globalMultiplier = Math.max(0.0d, multiplier);
    }

    public double globalMultiplier() {
        return globalMultiplier;
    }

    public int levelOf(IslandProfile island) {
        return config.curve().levelFor(island.islandXp());
    }

    public LevelTrack track(IslandProfile island) {
        return new LevelTrack(config.curve(), island.islandXp());
    }

    /** Adds raw island XP (already multiplied) and persists the new total. */
    public LevelUpResult addXp(IslandProfile island, long amount) {
        LevelTrack track = track(island);
        LevelUpResult result = track.addXp(com.apexmc.skyblock.core.util.Numbers.applyMultiplier(amount, globalMultiplier));
        if (result.changed()) {
            island.islandXp(track.totalXp());
        }
        return result;
    }

    /** Adds island XP derived from a configured gameplay source. */
    public LevelUpResult addXpFromSource(IslandProfile island, String sourceId, long units) {
        if (units <= 0) {
            int level = levelOf(island);
            return new LevelUpResult(level, level, island.islandXp(), 0L);
        }
        double perUnit = config.xpPerSource(sourceId);
        long amount = (long) Math.floor(perUnit * units);
        return addXp(island, amount);
    }

    public LevelUpResult setLevel(IslandProfile island, int level) {
        LevelTrack track = track(island);
        LevelUpResult result = track.setLevel(level);
        island.islandXp(track.totalXp());
        return result;
    }

    public LevelUpResult setXp(IslandProfile island, long xp) {
        LevelTrack track = track(island);
        LevelUpResult result = track.setTotalXp(xp);
        island.islandXp(track.totalXp());
        return result;
    }

    public LevelUpResult removeXp(IslandProfile island, long xp) {
        LevelTrack track = track(island);
        LevelUpResult result = track.removeXp(xp);
        island.islandXp(track.totalXp());
        return result;
    }

    /** True when the island level (or an explicit grant) unlocked the feature. */
    public boolean isUnlocked(IslandProfile island, String unlockId) {
        String id = Ids.normalize(unlockId);
        if (island.grantedUnlocks().contains(id)) {
            return true;
        }
        return config.unlockLevel(id).map(required -> levelOf(island) >= required).orElse(false);
    }

    /** Unlocks that became available between two levels, in order. */
    public List<String> unlocksBetween(int fromExclusive, int toInclusive) {
        List<String> unlocks = new ArrayList<>();
        for (int level = fromExclusive + 1; level <= toInclusive; level++) {
            config.level(level).ifPresent(definition -> unlocks.addAll(definition.unlocks()));
        }
        return unlocks;
    }

    /** Levels reached between two levels, used for reward payouts. */
    public List<IslandLevel> levelsBetween(int fromExclusive, int toInclusive) {
        List<IslandLevel> levels = new ArrayList<>();
        for (int level = fromExclusive + 1; level <= toInclusive; level++) {
            config.level(level).ifPresent(levels::add);
        }
        return levels;
    }
}
