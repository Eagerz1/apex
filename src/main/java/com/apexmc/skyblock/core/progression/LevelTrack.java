package com.apexmc.skyblock.core.progression;

import com.apexmc.skyblock.core.util.Numbers;

/**
 * Mutable XP holder bound to an {@link XpCurve}. Every levelled system in the
 * plugin (island, role, mastery, companion, tool node) reuses this class so the
 * XP/level maths exists exactly once.
 */
public final class LevelTrack {

    private final XpCurve curve;
    private long totalXp;

    public LevelTrack(XpCurve curve) {
        this(curve, 0L);
    }

    public LevelTrack(XpCurve curve, long totalXp) {
        this.curve = curve;
        this.totalXp = Numbers.clamp(totalXp, 0L, curve.totalXpCap());
    }

    public XpCurve curve() {
        return curve;
    }

    public long totalXp() {
        return totalXp;
    }

    public int level() {
        return curve.levelFor(totalXp);
    }

    public boolean maxed() {
        return level() >= curve.maxLevel();
    }

    public long xpIntoLevel() {
        return curve.progressInLevel(totalXp);
    }

    public long xpToNextLevel() {
        int level = level();
        if (level >= curve.maxLevel()) {
            return 0L;
        }
        return curve.cumulativeXp(level + 1) - totalXp;
    }

    public double progress() {
        return curve.progressFraction(totalXp);
    }

    /** Adds XP (negative and zero amounts are ignored) and reports the outcome. */
    public LevelUpResult addXp(long amount) {
        int before = level();
        if (amount <= 0 || maxed()) {
            return new LevelUpResult(before, before, totalXp, 0L);
        }
        long cap = curve.totalXpCap();
        long updated = Numbers.clamp(Numbers.safeAdd(totalXp, amount), 0L, cap);
        long applied = updated - totalXp;
        totalXp = updated;
        return new LevelUpResult(before, level(), totalXp, applied);
    }

    /** Admin operation: overwrite total XP. */
    public LevelUpResult setTotalXp(long amount) {
        int before = level();
        long previous = totalXp;
        totalXp = Numbers.clamp(amount, 0L, curve.totalXpCap());
        return new LevelUpResult(before, level(), totalXp, totalXp - previous);
    }

    /** Admin operation: jump straight to a level (XP set to that level's threshold). */
    public LevelUpResult setLevel(int level) {
        return setTotalXp(curve.cumulativeXp(Numbers.clamp(level, 0, curve.maxLevel())));
    }

    /** Admin operation: remove XP without dropping below zero. */
    public LevelUpResult removeXp(long amount) {
        if (amount <= 0) {
            int level = level();
            return new LevelUpResult(level, level, totalXp, 0L);
        }
        return setTotalXp(totalXp - amount);
    }
}
