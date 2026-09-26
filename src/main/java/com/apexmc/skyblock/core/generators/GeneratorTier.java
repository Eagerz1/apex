package com.apexmc.skyblock.core.generators;

import com.apexmc.skyblock.core.skills.WeightedTable;
import java.util.List;

/**
 * One generator tier. Tiers differ in output table, speed and unlocked
 * mechanics, not only in numbers.
 *
 * @param tier                 tier number (1-based, contiguous)
 * @param displayName          GUI name
 * @param intervalTicks        ticks between productions
 * @param outputs              weighted output material table
 * @param outputAmount         items produced per cycle
 * @param requiredIslandLevel  island level needed to upgrade to this tier
 * @param requiredUnlock       island unlock id needed ("" = none)
 * @param tokenCost            token cost to upgrade to this tier
 * @param moneyCost            money cost to upgrade to this tier
 * @param islandXpPerCycle     island XP granted per production cycle
 * @param masteryXpPerCycle    generator mastery XP granted per cycle
 * @param abilities            mechanic identifiers unlocked by this tier
 */
public record GeneratorTier(int tier,
                            String displayName,
                            int intervalTicks,
                            WeightedTable<String> outputs,
                            int outputAmount,
                            int requiredIslandLevel,
                            String requiredUnlock,
                            long tokenCost,
                            double moneyCost,
                            long islandXpPerCycle,
                            long masteryXpPerCycle,
                            List<String> abilities) {

    public GeneratorTier {
        abilities = List.copyOf(abilities);
    }

    public boolean hasAbility(String ability) {
        return abilities.stream().anyMatch(entry -> entry.equalsIgnoreCase(ability));
    }
}
