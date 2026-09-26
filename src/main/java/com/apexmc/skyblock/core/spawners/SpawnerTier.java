package com.apexmc.skyblock.core.spawners;

import java.util.List;

/**
 * One spawner tier.
 *
 * @param tier                tier number (1-based, contiguous)
 * @param displayName         GUI name
 * @param entityType          entity key spawned (resolved by the Bukkit layer)
 * @param intervalTicks       ticks between spawn attempts
 * @param spawnAmount         entities per attempt (per stacked spawner)
 * @param maxNearbyEntities   hard cap of living entities near the spawner
 * @param requiredIslandLevel island level required
 * @param requiredUnlock      island unlock required ("" = none)
 * @param tokenCost           token cost of upgrading to this tier
 * @param moneyCost           money cost of upgrading to this tier
 * @param slayerXpBonus       extra slayer XP multiplier for mobs from this tier
 * @param abilities           mechanic identifiers unlocked by this tier
 */
public record SpawnerTier(int tier,
                          String displayName,
                          String entityType,
                          int intervalTicks,
                          int spawnAmount,
                          int maxNearbyEntities,
                          int requiredIslandLevel,
                          String requiredUnlock,
                          long tokenCost,
                          double moneyCost,
                          double slayerXpBonus,
                          List<String> abilities) {

    public SpawnerTier {
        abilities = List.copyOf(abilities);
    }
}
