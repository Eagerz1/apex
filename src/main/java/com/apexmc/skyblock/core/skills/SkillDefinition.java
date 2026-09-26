package com.apexmc.skyblock.core.skills;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Everything a skill needs: which role and mastery category it feeds, the
 * per-target payouts, the rare drop table and its milestones.
 */
public record SkillDefinition(SkillType type,
                              String roleId,
                              String masteryCategoryId,
                              Map<String, SkillSource> sources,
                              WeightedTable<SkillDrop> rareDrops,
                              double rareDropChance,
                              List<SkillMilestone> milestones) {

    public SkillDefinition {
        sources = Map.copyOf(sources);
        milestones = List.copyOf(milestones);
    }

    public Optional<SkillSource> source(String key) {
        return key == null ? Optional.empty() : Optional.ofNullable(sources.get(key.toUpperCase(java.util.Locale.ROOT)));
    }
}
