package com.apexmc.skyblock.core.skills;

/**
 * Configured payout for a single gameplay target: a mined block, a harvested
 * crop, a felled log, a caught fish or a slain entity.
 *
 * @param key                material / entity key (upper case)
 * @param roleXp             base role XP
 * @param islandXp           base island XP
 * @param masteryXp          base mastery XP
 * @param requiredIslandLevel island level required before the target yields anything
 * @param tokenChance        chance (0..1) of a token drop
 * @param tokenAmount        tokens granted when the chance procs
 */
public record SkillSource(String key,
                          long roleXp,
                          long islandXp,
                          long masteryXp,
                          int requiredIslandLevel,
                          double tokenChance,
                          long tokenAmount) {
}
