package com.apexmc.skyblock.core.skills;

/**
 * A special / rare drop produced by a skill.
 *
 * @param id                  drop identifier
 * @param material            material key given to the player
 * @param amount              stack size
 * @param requiredIslandLevel island level gate
 * @param tokens              bonus tokens granted with the drop
 */
public record SkillDrop(String id, String material, int amount, int requiredIslandLevel, long tokens) {
}
