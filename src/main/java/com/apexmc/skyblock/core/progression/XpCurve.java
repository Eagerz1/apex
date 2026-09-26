package com.apexmc.skyblock.core.progression;

import com.apexmc.skyblock.core.config.Cfg;
import com.apexmc.skyblock.core.config.ConfigException;
import com.apexmc.skyblock.core.util.Numbers;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Cumulative XP requirement curve for any levelled track (island, role, mastery,
 * companion). Three fully configurable modes are supported:
 *
 * <ul>
 *   <li>{@code table}   - explicit per-level requirements</li>
 *   <li>{@code linear}  - {@code base + step * (level - 1)}</li>
 *   <li>{@code geometric} - {@code base * growth^(level - 1)}</li>
 * </ul>
 *
 * <p>The curve always describes the XP needed to go from {@code level} to
 * {@code level + 1}; cumulative totals are derived and cached.</p>
 */
public final class XpCurve {

    private final int maxLevel;
    private final long[] perLevel;     // index 0 = cost of reaching level 1 from level 0
    private final long[] cumulative;   // cumulative[i] = total xp needed to be level i

    private XpCurve(int maxLevel, long[] perLevel) {
        this.maxLevel = maxLevel;
        this.perLevel = perLevel;
        this.cumulative = new long[maxLevel + 1];
        long total = 0;
        for (int level = 1; level <= maxLevel; level++) {
            total = Numbers.safeAdd(total, perLevel[level - 1]);
            cumulative[level] = total;
        }
    }

    public static XpCurve table(List<Long> requirements) {
        if (requirements.isEmpty()) {
            throw new IllegalArgumentException("xp curve table must not be empty");
        }
        long[] perLevel = new long[requirements.size()];
        for (int i = 0; i < requirements.size(); i++) {
            long value = requirements.get(i);
            if (value <= 0) {
                throw new IllegalArgumentException("xp requirement for level " + (i + 1) + " must be positive");
            }
            perLevel[i] = value;
        }
        return new XpCurve(perLevel.length, perLevel);
    }

    public static XpCurve linear(int maxLevel, long base, long step) {
        validateMaxLevel(maxLevel);
        long[] perLevel = new long[maxLevel];
        for (int level = 1; level <= maxLevel; level++) {
            perLevel[level - 1] = Math.max(1L, base + step * (level - 1L));
        }
        return new XpCurve(maxLevel, perLevel);
    }

    public static XpCurve geometric(int maxLevel, long base, double growth) {
        validateMaxLevel(maxLevel);
        if (growth < 1.0d) {
            throw new IllegalArgumentException("geometric growth must be >= 1.0");
        }
        long[] perLevel = new long[maxLevel];
        for (int level = 1; level <= maxLevel; level++) {
            double value = base * Math.pow(growth, level - 1.0d);
            perLevel[level - 1] = Math.max(1L, (long) Math.floor(value));
        }
        return new XpCurve(maxLevel, perLevel);
    }

    /** Builds a curve from a configuration section. */
    public static XpCurve fromConfig(Cfg cfg) {
        String type = cfg.getString("type", "geometric").toLowerCase(Locale.ROOT);
        switch (type) {
            case "table" -> {
                List<String> raw = cfg.getStringList("requirements");
                if (raw.isEmpty()) {
                    throw new ConfigException(cfg.path(), "curve type 'table' requires a non-empty 'requirements' list");
                }
                List<Long> values = new ArrayList<>(raw.size());
                for (String entry : raw) {
                    try {
                        values.add(Long.parseLong(entry.trim()));
                    } catch (NumberFormatException ex) {
                        throw new ConfigException(cfg.path() + ".requirements", "'" + entry + "' is not a whole number");
                    }
                }
                try {
                    return table(values);
                } catch (IllegalArgumentException ex) {
                    throw new ConfigException(cfg.path(), ex.getMessage());
                }
            }
            case "linear" -> {
                return wrap(cfg, () -> linear(cfg.getInt("max-level"), cfg.getLong("base"), cfg.getLong("step", 0L)));
            }
            case "geometric" -> {
                return wrap(cfg, () -> geometric(cfg.getInt("max-level"), cfg.getLong("base"), cfg.getDouble("growth", 1.15d)));
            }
            default -> throw new ConfigException(cfg.path() + ".type",
                    "unknown curve type '" + type + "' (expected table, linear or geometric)");
        }
    }

    private static XpCurve wrap(Cfg cfg, java.util.function.Supplier<XpCurve> supplier) {
        try {
            return supplier.get();
        } catch (IllegalArgumentException ex) {
            throw new ConfigException(cfg.path(), ex.getMessage());
        }
    }

    private static void validateMaxLevel(int maxLevel) {
        if (maxLevel < 1 || maxLevel > 10_000) {
            throw new IllegalArgumentException("max-level must be between 1 and 10000 (got " + maxLevel + ")");
        }
    }

    public int maxLevel() {
        return maxLevel;
    }

    /** XP required to advance from {@code level} to {@code level + 1}, or 0 at max level. */
    public long xpForNextLevel(int level) {
        if (level < 0 || level >= maxLevel) {
            return 0L;
        }
        return perLevel[level];
    }

    /** Total XP required to have reached exactly {@code level}. */
    public long cumulativeXp(int level) {
        int clamped = Numbers.clamp(level, 0, maxLevel);
        return cumulative[clamped];
    }

    /** Resolves the level reached with a given total XP amount. */
    public int levelFor(long totalXp) {
        if (totalXp <= 0) {
            return 0;
        }
        int level = 0;
        while (level < maxLevel && totalXp >= cumulative[level + 1]) {
            level++;
        }
        return level;
    }

    /** XP accumulated inside the current level. */
    public long progressInLevel(long totalXp) {
        int level = levelFor(totalXp);
        if (level >= maxLevel) {
            return 0L;
        }
        return Math.max(0L, totalXp - cumulative[level]);
    }

    /** Fraction (0..1) of progress towards the next level; 1.0 when maxed. */
    public double progressFraction(long totalXp) {
        int level = levelFor(totalXp);
        if (level >= maxLevel) {
            return 1.0d;
        }
        long needed = xpForNextLevel(level);
        if (needed <= 0) {
            return 1.0d;
        }
        return Numbers.clamp(progressInLevel(totalXp) / (double) needed, 0.0d, 1.0d);
    }

    /** Total XP cap of the curve; XP above this is never stored. */
    public long totalXpCap() {
        return cumulative[maxLevel];
    }
}
