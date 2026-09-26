package com.apexmc.skyblock.core.generators;

import java.util.List;
import java.util.Optional;

/** A configured generator type with its ordered tier list. */
public record GeneratorType(String id, String displayName, String icon, int maxPerIsland, List<GeneratorTier> tiers) {

    public GeneratorType {
        tiers = List.copyOf(tiers);
    }

    public int maxTier() {
        return tiers.size();
    }

    public Optional<GeneratorTier> tier(int tier) {
        if (tier < 1 || tier > tiers.size()) {
            return Optional.empty();
        }
        return Optional.of(tiers.get(tier - 1));
    }
}
