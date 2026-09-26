package com.apexmc.skyblock.core.skills;

import java.util.List;

/**
 * A count based milestone ("mine 10,000 blocks") granting rewards and unlocks.
 */
public record SkillMilestone(String id, long amount, long tokenReward, long islandXpReward, List<String> unlocks) {

    public SkillMilestone {
        unlocks = List.copyOf(unlocks);
    }
}
