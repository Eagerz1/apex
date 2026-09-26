package com.apexmc.skyblock.core.progression;

/**
 * Immutable outcome of adding XP to any levelled track.
 *
 * @param previousLevel level before the gain
 * @param newLevel      level after the gain
 * @param totalXp       stored total XP after the gain (already capped)
 * @param gainedXp      XP actually applied (0 when capped or rejected)
 */
public record LevelUpResult(int previousLevel, int newLevel, long totalXp, long gainedXp) {

    public boolean leveledUp() {
        return newLevel > previousLevel;
    }

    public int levelsGained() {
        return Math.max(0, newLevel - previousLevel);
    }

    public boolean changed() {
        return gainedXp > 0;
    }
}
