package com.apexmc.skyblock.core.mastery;

import com.apexmc.skyblock.core.progression.XpCurve;
import java.util.List;
import java.util.Map;

/**
 * A mastery specialisation track (Mining, Farming, Fishing, Slayer, Logging,
 * Generators, ...). New categories only require a configuration entry.
 */
public record MasteryCategory(String id,
                              String displayName,
                              String icon,
                              XpCurve curve,
                              List<MasteryUnlock> unlocks,
                              Map<String, Double> xpSources) {

    public MasteryCategory {
        unlocks = List.copyOf(unlocks);
        xpSources = Map.copyOf(xpSources);
    }

    public int maxLevel() {
        return curve.maxLevel();
    }

    public List<MasteryUnlock> unlocksUpTo(int level) {
        return unlocks.stream().filter(unlock -> unlock.level() <= level).toList();
    }

    /** Sum of every unlocked passive of a given type. */
    public double passiveValue(int level, String type) {
        double total = 0.0d;
        for (MasteryUnlock unlock : unlocksUpTo(level)) {
            if (unlock.type().equalsIgnoreCase(type)) {
                total += unlock.value();
            }
        }
        return total;
    }

    public double xpPerSource(String sourceId) {
        return xpSources.getOrDefault(sourceId.toLowerCase(java.util.Locale.ROOT), 0.0d);
    }
}
