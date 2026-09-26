package com.apexmc.skyblock.core.spawners;

import java.util.List;
import java.util.Optional;

/** A configured spawner type with its ordered tier list. */
public record SpawnerType(String id, String displayName, String icon, int maxPerIsland, int maxStackSize, List<SpawnerTier> tiers) {

    public SpawnerType {
        tiers = List.copyOf(tiers);
    }

    public int maxTier() {
        return tiers.size();
    }

    public Optional<SpawnerTier> tier(int tier) {
        if (tier < 1 || tier > tiers.size()) {
            return Optional.empty();
        }
        return Optional.of(tiers.get(tier - 1));
    }
}
